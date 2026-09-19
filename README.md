# NexusPurgatory

List specific players in `config.yml` -- by Java username and/or Xbox/Bedrock gamertag, and
**only** those players -- and they're placed under an escalating curse: capped health, hunger
that never recovers, mob damage that's near-guaranteed (or, by default, absolutely guaranteed)
lethal, constant slowness, a jumbled inventory, their most valuable item quietly deleted every
few minutes, and short bursts of extra debuffs that land more often the longer they stay online.
To release someone, remove their line from `config.yml` and run `/nexuspurgatory reload` --
that's the plugin's entire off switch. There is deliberately no in-game pardon command, no
"register" command, and no way for this plugin to ever touch a player whose name isn't listed.

**Nobody not explicitly named in `config.yml`'s `targets` list is ever affected, by any part of
this plugin, for any reason.** Every single effect below funnels through one check --
`CurseTargetMatcher.isTargeted` -- before it does anything at all.

## What it does

**Health capped at one heart (configurable).** `capHealthPoints` (default `2.0`, one heart) is
applied by lowering the player's `GENERIC_MAX_HEALTH` attribute itself, not by cancelling healing
events -- Bukkit clamps current health to max automatically, so the cap holds against every regen
source (natural regen, potions, even a golden apple) with no extra bookkeeping. The instant
someone's no longer cursed (config edit + reload, or -- with the default `mobDamage` settings --
simply because they died and there's nothing left to cap) their original max health is restored.

**Hunger drained back down every 45 seconds (configurable).** Food level is force-set to
`hunger.drainToLevel` (default `1` -- just above instant starvation damage, so the real danger
stays in the health cap and mob damage rather than a flat, boring starvation tick) and saturation
zeroed, so it never has a chance to climb back up on its own between drains.

**Mob damage is near-guaranteed, or by default absolutely guaranteed, lethal.** Any hit from a
hostile mob (melee, or an arrow/projectile a hostile mob fired) against a cursed player has a
`mobDamage.lethalityChance` chance (default `1.0`, i.e. always) of being amplified to at least
their current health -- guaranteeing the hit finishes them. Combined with the one-heart cap, most
hits are already fatal on their own; this is what closes the gap on the occasional low-roll hit
that otherwise wouldn't quite finish the job.

**Constant slowness.** A low-amplifier Slowness effect (`slowness.amplifier`, default `0` =
Slowness I) is silently reapplied every `slowness.refreshIntervalSeconds` (default 5) with a
duration comfortably longer than that interval, so it's never seen to flicker off between
reapplications. Deliberately kept mild and constant -- a background annoyance, not the main event.

**Inventory jumbled every 45 seconds (configurable).** The 36 carryable storage slots (main
inventory + hotbar) are shuffled into a random order. Armor and off-hand are never touched.

**Their most valuable item vanishes every 5 minutes (configurable).** Once per
`itemDrain.intervalSeconds` (default 300), the single highest-scoring item in a cursed player's
storage slots is deleted outright, with a taunting chat message (`itemDrain.tauntMessage`, `%item%`
substituted). "Most valuable" is a rarity/power heuristic, not real market price -- see "Design
choices worth recording" below for exactly how it's scored, and why.

**Curse pulses: intermittent extra debuffs, more frequent the longer someone stays cursed.**
Every `cursePulse.intervalSeconds` (default 90, scaled down by escalation -- see next), a cursed
player gets a `cursePulse.durationSeconds`-long burst of one random extra debuff: Nausea,
Blindness, Mining Fatigue, or Weakness. This keeps the baseline misery from becoming something a
player could get numb to.

**Escalation: it gets worse the longer they stay online and cursed.** `escalation.
stageThresholdMinutes` (default `[5, 15, 30]`) defines how many minutes of *continuous* cursed
time it takes to reach each escalation stage; each stage shrinks the curse-pulse interval by
`escalation.pulseIntervalReductionPerStageSeconds` (default 15), floored at `escalation.
minPulseIntervalSeconds` (default 20). This is the direct mechanism behind "not so fast they rage
quit, but absolutely to where they will" -- a freshly-cursed player starts at the base severity;
someone who stays on for half an hour is getting hit noticeably more often. Escalation resets on
logout (see "What this deliberately doesn't do").

**A boss bar only the cursed player can see.** Shows elapsed cursed time this session and current
escalation stage -- a constant, impossible-to-mistake-for-a-glitch reminder that it isn't
stopping. Turn it off with `bossBar.enabled: false`.

## Design choices worth recording

- **Targeting is re-evaluated live against `config.yml`, every single check -- never cached, never
  a permanent registry.** This is the opposite choice from NexusPrison's UUID-based registry (a
  sentence that's meant to survive a config edit by design). Here, the moment an admin removes a
  name from `targets` and runs `/nexuspurgatory reload`, every effect stops on that player's very
  next tick. Given how severe this plugin's effects are, that's the whole safety valve -- there is
  no separate pardon command, because none is needed, and none would be safer than "just edit the
  one list that controls everything."
- **`CurseTargetMatcher.isTargeted` is the single gate every task and listener calls first.**
  `CurseTargets.checkAndTrack` wraps it so all seven periodic tasks and the mob-damage listener
  agree on the exact same answer every tick, and keep the per-session "how long have they been
  cursed" clock consistent no matter which task happens to run first in a given tick.
- **No separate Bedrock-detection code.** Floodgate already presents a Bedrock player's gamertag
  transparently as `Player#getName()`, so `javaUsername`/`xboxGamertag` matching against that one
  field-set (case-insensitive, either field) already covers both platforms -- same observation
  NexusPrison's `PrisonerMatcher` already relies on.
- **Health capping via `AttributeInstance#setBaseValue`, not by cancelling regen events.** Setting
  the max-health attribute's base value directly means Bukkit's own health-clamping keeps current
  health from ever exceeding it, for free, against any healing source -- rather than this plugin
  having to individually intercept and cancel every possible way health could climb (natural
  regen, splash potions, golden apples, other plugins).
- **Item-value scoring is a name-pattern heuristic, not an exhaustive material table.** `Material`
  has hundreds of constants in real Paper; `ItemValueHeuristic` matches by substring against tier
  keywords (netherite > totem of undying > nether star > elytra > diamond > emerald > enchanted
  book > gold > iron > copper/stone > wood > leather, plus a small positive floor for anything
  unrecognized so an item-drain cycle is never a silent no-op), and adds a flat per-enchantment,
  per-level bonus on top. It's a heuristic, not appraisal -- good enough to reliably pick "the
  thing that'll actually hurt to lose" without hand-maintaining a table of hundreds of entries.
- **Escalation and the per-session cursed-time clock are tracked in memory only, never persisted,
  and reset on logout.** A player who logs off and back on starts back at the base escalation
  stage rather than picking up where they left off. This is a deliberate v1 scope choice, not an
  oversight -- see "What this deliberately doesn't do."
- **Scheduler intervals are fixed at `onEnable()`, not re-read from config on every tick.** Health,
  curse-pulse, and the boss bar all run every second regardless of config (the health cap needs
  per-tick precision to stick against fast regen sources, and curse-pulse does its own internal
  per-player interval math so it must be invoked frequently to catch each player's individual due
  time). Hunger, slowness, inventory-jumble, and item-drain instead run on Bukkit scheduler tasks
  registered with their own configured period. `/nexuspurgatory reload` still applies every other
  config change immediately -- most importantly the `targets` list itself, and every severity
  value each task reads fresh off `CurseState.config()` on every run. Only the *interval* fields
  need a restart; see "What this deliberately doesn't do."

## What this deliberately doesn't do

- **No in-game way to add or remove a target.** `config.yml`'s `targets` list is the only gate,
  on purpose -- see "Design choices worth recording" above. `/nexuspurgatory` only ever has
  `status` and `reload`.
- **Escalation and the cursed-time clock reset on logout, not persisted across sessions or a
  server restart.** A player who's been cursed for 25 minutes, logs off, and rejoins starts back
  at stage 0 rather than resuming mid-escalation. This keeps the implementation simple and avoids
  a second on-disk file just for timing state that arguably *should* reset with a fresh session
  anyway -- left as a v1 scope choice rather than guessed at; a future version could persist it if
  that turns out to matter in practice.
- **A config change to any *interval* field needs a server restart, not just `/nexuspurgatory
  reload`.** Bukkit doesn't support re-periodizing an already-scheduled repeating task. Every
  other setting -- the `targets` list, health cap, drain amounts, lethality chance, taunt message,
  escalation thresholds/reduction, boss bar appearance -- takes effect immediately on reload.
- **Restoring original max health if the plugin's own memory of it was lost across a server
  restart isn't guaranteed.** `originalMaxHealth` is an in-memory map, not persisted. If a player
  is un-targeted while the *server* (not just the player) is offline across a restart, and they
  were mid-curse when it went down, this plugin has no record of what to restore them to on
  rejoin, since it never saw them cursed this run. In practice this is a narrow edge case (an
  admin editing config.yml while the server itself is down, for a player who was actively cursed
  at the moment of the previous shutdown) -- flagged here rather than silently risking a
  permanently-capped max health with no clear cause.
- **No cross-plugin conflict handling.** If another plugin also touches `GENERIC_MAX_HEALTH`,
  food level, or potion effects for the same player, the two will fight each other. Not addressed
  here -- outside this plugin's own scope.

## Config

See `config.yml` -- every setting above is there, commented, including the `targets` list format.

## Commands & permissions

- `/nexuspurgatory status` -- lists every currently-online cursed player, how long they've been
  cursed this session, and their escalation stage. Admin only.
- `/nexuspurgatory reload` -- reloads `config.yml`. Admin only.
- Permission: `nexuspurgatory.admin` (default: op) -- required for both subcommands; there is no
  self-status option, since a cursed player already can't help but notice.
- Aliases: `/purgatory`, `/nexuscurse`.

## If anything fails to compile against a real Paper 1.21.x jar

This was written and compile-verified against a hand-written stub of the Paper API (this sandbox
has no network access to Maven Central / repo.papermc.io), modeled closely on the exact API shape
already confirmed correct while building the rest of this plugin family. This project's own new
stub surface:

- **`AttributeInstance#getBaseValue()`/`setBaseValue(double)`, and a real `SimpleAttributeInstance`
  backing it.** Ported from NexusCompanions' stub tree, which already had the correct shape for
  this. `Player#getAttribute(Attribute)` previously always returned `null` in this family's shared
  stub; fixed here to actually back a `GENERIC_MAX_HEALTH` instance (defaulting to `20.0`, vanilla's
  own default) the first time it's asked for.
- **`Player#getFoodLevel()`/`setFoodLevel(int)`/`getSaturation()`/`setSaturation(float)`.** The
  inherited stub previously hardcoded `getFoodLevel()` to always return `20` with no setter at all,
  and had no saturation methods whatsoever -- fixed to real, stateful fields, first genuinely
  needed by this plugin in this family.
- **`Player#getHealth()`/`setHealth(double)`.** Previously no-ops always returning `20.0`; fixed to
  a real field that clamps to the current `GENERIC_MAX_HEALTH` value on write, matching real
  Bukkit's own documented clamping behavior.
- **`EntityDamageEvent#getDamage()`/`setDamage(double)`.** Previously entirely absent from this
  family's stub (only `getEntity()`/`getCause()`/`isCancelled()`/`setCancelled()` existed -- no
  earlier plugin needed to read or amplify the actual damage amount). Real Bukkit's
  `getFinalDamage()` (post armor/enchantment/potion reduction) is real API too but deliberately NOT
  modeled here, since this plugin only ever reads/writes the base amount.
- **`Monster` marker interface (new).** A bare `public interface Monster { }` with no supertype --
  this stub tree models `LivingEntity` as a concrete class, and an interface can't extend a class,
  so `Monster` can't mirror real Bukkit's actual inheritance chain (`Monster extends Mob extends
  LivingEntity extends ...`) exactly. `instanceof Monster` still works correctly against any real
  concrete hostile-mob type real Bukkit provides, since Java's `instanceof` against an unrelated,
  non-final interface type is always legal to check -- this is a stub-modeling simplification, not
  a behavior change.
- **`PotionEffectType.SLOWNESS`/`MINING_FATIGUE`/`WEAKNESS` (new constants).** Believed correct and
  stable (long-standing Bukkit API); this family's stub previously only had NAUSEA, BLINDNESS,
  REGENERATION, SATURATION, and HASTE, none of which this plugin's slowness effect or curse-pulse
  pool could reuse as-is.
- **`Bukkit#createBossBar(...)`/`Server#createBossBar(...)` and the `BossBar`/`BossBarImpl`/
  `BarColor`/`BarStyle`/`BarFlag` types.** Carried over unchanged from NexusChronicle, the first
  plugin in this family to need the boss bar API -- already proven correct there.

None of the above affects this plugin's own pure logic (target matching, item-value scoring, the
inventory shuffle, or escalation math) -- see `CHANGES.md` for how those pieces were verified
instead.

## Build

```
mvn clean package
```

Produces `target/NexusPurgatory-0.1.0.jar`. Requires Java 21, Paper 1.21+, and network access to
`repo.papermc.io`.

`src/test/java/.../NexusPurgatoryTest.java` is a standalone verification tool (plain `main()`, no
JUnit) written against this project's own sandbox stub of the Bukkit API, not the real one.
`mvn clean package` skips compiling it entirely (`maven.test.skip=true` in `pom.xml`, same pattern
this whole plugin family uses) rather than trying to satisfy real Bukkit's much heavier
`Plugin`/`FileConfiguration` surface by hand. To actually run it, point `javac`/`java` at this
project's own `stubs/` directory instead of the real paper-api jar -- it's not meant to compile
against the real one.
