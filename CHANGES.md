# NexusPurgatory changelog

## v0.1.0 -- initial release

Brand-new plugin, built from scratch: an escalating, config-target-only curse system (health cap,
hunger drain, near/absolutely-lethal mob damage, slowness, inventory jumbling, item drain,
intermittent extra debuffs, and a boss bar), scoped so that it can never affect a player whose
name isn't explicitly listed in `config.yml`. See `README.md` for the full feature list and the
design reasoning.

### Design choices worth recording

- **Targeting is re-evaluated live against `config.yml` every single check, never cached, never a
  permanent registry.** `CurseTargetMatcher.isTargeted` is the one gate every task and the
  mob-damage listener consult before applying any effect at all; `CurseTargets.checkAndTrack`
  centralizes the call so every task agrees and keeps the per-session cursed-time clock
  consistent. This is a deliberate divergence from NexusPrison's own permanent UUID registry (a
  sentence that's meant to survive a config edit by design) -- here, the entire "off switch" is
  editing `targets` and running `/nexuspurgatory reload`, on purpose, given how severe this
  plugin's effects are.
- **Health capping via `AttributeInstance#setBaseValue`, not event cancellation.** Setting
  `GENERIC_MAX_HEALTH`'s base value directly means Bukkit's own clamping keeps current health from
  exceeding it automatically, against any regen source, with no need to individually intercept
  every possible way health could climb.
- **Item-value scoring is a name-pattern heuristic (`ItemValueHeuristic`), not an exhaustive
  `Material` table.** Tier-matches by substring (netherite/totem/nether star/elytra/diamond/
  emerald/enchanted book/gold/iron/copper-stone/wood/leather, small positive floor otherwise) plus
  a flat per-enchantment-per-level bonus. Chosen per the user's own stated preference for a
  rarity/power heuristic over, say, always targeting the single highest item-count stack.
- **Escalation (`CurseEscalation`) is pure interval-scaling math, independent of any specific
  effect.** `stageFor` turns "how long has this player been continuously cursed" into a stage
  number against a sorted threshold list; `scaleInterval` shrinks a base interval per stage,
  floored at a configured minimum. Currently only `CursePulseTask` consumes it, but nothing about
  it is curse-pulse-specific -- a future effect could reuse the same stage number.
- **The per-session cursed-time clock (`CurseState.curseStartEpochSecond`) and escalation both
  reset on logout (`CurseSessionListener`), by design -- not persisted.** Respawn is zero-mercy
  (the curse is active the instant a targeted player respawns or rejoins, confirmed directly by
  the user), but the *escalation* clock specifically restarts each session; see README's "what
  this deliberately doesn't do" for why that's a scope choice, not an oversight.
- **`originalMaxHealth` is remembered (in memory, per UUID) the first tick a player is seen
  cursed, and restored the first tick they're seen not-cursed** -- `CurseState.
  rememberOriginalMaxHealth` uses `putIfAbsent` specifically so repeated cursed ticks never
  overwrite the true original with the already-capped value.
- **Scheduler task periods are fixed at `onEnable()` from the config values in effect at startup,
  not re-read every tick.** Health, curse-pulse, and the boss bar task all run every second
  unconditionally (health needs per-tick precision against fast regen; curse-pulse does its own
  internal per-player interval bookkeeping and must be invoked often enough to catch each due
  time). Hunger, slowness, inventory-jumble, and item-drain are each registered with their own
  configured period directly. Every task still reads current severity values fresh off
  `CurseState.config()` on each run, so `/nexuspurgatory reload` applies immediately to
  everything except the interval fields themselves (which need a restart -- Bukkit has no API to
  re-periodize an already-scheduled repeating task).
- **`CurseMobDamageListener` amplifies damage up to `player.getHealth()`, not to some fixed large
  number.** `Math.max(event.getDamage(), player.getHealth())` guarantees exactly enough damage to
  be lethal without wildly overshooting into, e.g., logged "impossible" damage values some
  anti-cheat or logging plugin might flag as suspicious.
- **No `BedrockUtil` in this plugin.** Unlike NexusPrison (which needed to detect and log the
  connecting platform for its own registration flow), NexusPurgatory only ever needs to know "does
  this name match," and Floodgate already presents a Bedrock player's gamertag transparently as
  `Player#getName()` -- so the existing `javaUsername`/`xboxGamertag` field pair on
  `CurseTargetConfig`, matched case-insensitively against either field, already covers both
  platforms with no separate detection code.

### Sandbox stub additions

This project's `stubs/` started as a full copy of NexusStarter's own stub tree, plus the boss bar
API (`BarColor`/`BarStyle`/`BarFlag`/`BossBar`/`BossBarImpl`, `createBossBar(...)` on both
`Bukkit` and `Server`) carried over unchanged from NexusChronicle, the first plugin in this family
to need it. Beyond that, this plugin needed real gameplay-mechanic state no earlier plugin in the
family had exercised:

- **`Player#getAttribute(Attribute)` was fixed to actually back an attribute instance.** It
  previously always returned `null` in this family's inherited stub. Added a
  `Map<Attribute, AttributeInstance>` field (lazily populated via `computeIfAbsent`, defaulting
  `GENERIC_MAX_HEALTH` to vanilla's own `20.0`) plus `getBaseValue()`/`setBaseValue(double)` on
  the `AttributeInstance` interface and a new concrete `SimpleAttributeInstance`, ported from
  NexusCompanions' stub tree (which already had the correct shape for this).
- **`Player#getFoodLevel()`/`setFoodLevel(int)`/`getSaturation()`/`setSaturation(float)` were
  fixed to real, stateful fields.** Previously `getFoodLevel()` was hardcoded to always return
  `20` with no setter, and no saturation methods existed at all.
- **`Player#getHealth()`/`setHealth(double)` were fixed to a real field with real clamping.**
  Previously both were no-ops that always returned `20.0`. `setHealth` now clamps to the player's
  current `GENERIC_MAX_HEALTH` value, matching real Bukkit's own documented behavior -- this is
  what makes the health-cap task's approach (lower the max, let Bukkit clamp current health down
  to meet it) actually work in the stub the same way it would against a real server.
- **`EntityDamageEvent#getDamage()`/`setDamage(double)` (new).** Previously absent entirely from
  this family's stub -- no earlier plugin needed to read or amplify a damage event's actual
  amount. Real Bukkit's `getFinalDamage()` (post armor/enchantment/potion reduction) is real API
  too but was deliberately not modeled, since this plugin only ever reads/writes the base amount.
- **`Monster` marker interface (new).** A bare `public interface Monster { }` with no supertype.
  An early draft tried `interface Monster extends LivingEntity`, which doesn't compile -- this
  stub tree models `LivingEntity` as a concrete class, and interfaces can't extend classes. Caught
  and fixed before ever compiling. `instanceof Monster` still works correctly against any real
  concrete hostile-mob type in real Bukkit's actual inheritance chain, since checking `instanceof`
  against an unrelated, non-final interface type is always legal Java regardless of whether that
  interface is actually implemented by the checked type's real hierarchy.
- **`PotionEffectType.SLOWNESS`/`MINING_FATIGUE`/`WEAKNESS` (new constants).** This family's stub
  previously only had NAUSEA, BLINDNESS, REGENERATION, SATURATION, and HASTE -- none reusable
  as-is for this plugin's constant slowness effect or its curse-pulse debuff pool.

No stub bugs were found in the boss-bar surface carried over from NexusChronicle -- every change
above was either a genuine gap (API this plugin needed that no earlier plugin had exercised) or a
stub method that existed but was a hardcoded no-op because nothing had needed it to actually work
yet (`getHealth`/`setHealth`/`getFoodLevel`/`getAttribute`).

### Verification

No network access in this sandbox to Maven Central or repo.papermc.io, so this couldn't be built
against a real Paper jar directly (same constraint every plugin in this family has noted).
Instead:

- All 22 real plugin source classes (15 Bukkit-glue + 7 pure-logic) compile clean against the
  (extended) stub with `javac -Xlint:all -Werror` -- zero warnings, zero errors.
- A standalone test suite (`src/test/java/.../NexusPurgatoryTest.java`, plain `main()`-based, no
  JUnit dependency) exercises every pure-logic class directly: `CurseTargetConfig`'s validation
  (at least one identity field required, a blank-but-present field still accepted alongside a real
  one); `CurseTargetMatcher.isTargeted`'s case-insensitivity, matching against either field, and
  null/blank/empty-list safety; `ItemValueHeuristic.score`'s tier ordering (netherite > diamond >
  iron > wood), the small positive floor for an unrecognized material, enchantment contribution
  (present vs. absent, and scaling with level), and null-safety for both the material name and the
  enchantment map; `InventoryShuffler.shuffle`'s length preservation, non-mutation of the input
  array, exact multiset preservation (same elements, different order), null-entry (empty slot)
  preservation, single-element and empty-array edge cases, and confirmation across 20 seeded runs
  that it actually reorders rather than being an accidental no-op; `CurseEscalation.stageFor`'s
  boundary behavior at and just before/after each threshold plus the "caps at the number of
  thresholds" and "empty threshold list is always stage 0" edge cases, and `scaleInterval`'s
  per-stage reduction and floor clamping; and `MaterialNames.humanize`'s word-splitting/
  title-casing across one-, two-, and three-word materials, null/blank fallback, and a stray
  double-underscore not producing a double space. All 52 checks pass.
- Bukkit-glue classes (the seven scheduler tasks, `CurseMobDamageListener`,
  `CurseSessionListener`, `CurseBossBarManager`, `NexusPurgatoryPlugin`, `CurseCommand`) are
  verified only by the `-Xlint:all -Werror` compile check against the stub, matching the rest of
  this family's division of labor between unit-tested pure logic and compile-checked Bukkit glue.
- `pom.xml`, `plugin.yml`, and `config.yml` validated as well-formed XML/YAML both standalone and
  after unzipping the delivered archive.
- See `README.md`'s "if anything fails to compile against a real Paper 1.21.x jar" section for the
  specific new/fixed API surface that's uncertain because it couldn't be checked against a real
  jar.
