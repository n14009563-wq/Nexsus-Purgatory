package com.nexuscraft.nexuspurgatory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** One small, shared, mutable holder for the current {@link CurseConfig} plus the runtime
 *  bookkeeping every periodic task needs -- same "one shared point of truth" shape {@code
 *  GuideState}/{@code ChronicleState} use elsewhere in this family. {@code
 *  /nexuspurgatory reload} swaps the config field atomically; every task picks up the change on
 *  its very next tick.
 *
 *  <p>The per-player maps are deliberately session-only (never persisted): {@code
 *  curseStartEpochSecond} tracks when a player was FIRST seen cursed during their current online
 *  session (cleared on logout, or the moment they stop matching the target list), which is what
 *  {@link CurseEscalation} measures escalation against; {@code originalMaxHealth} remembers what
 *  a player's max-health attribute was before this plugin touched it, so it can be restored the
 *  instant they're no longer targeted (admin edit + reload, or they log out) rather than leaving
 *  them permanently capped at one heart. */
final class CurseState {

    private volatile CurseConfig config;
    private final Map<UUID, Long> curseStartEpochSecond = new ConcurrentHashMap<>();
    private final Map<UUID, Double> originalMaxHealth = new ConcurrentHashMap<>();

    CurseState(CurseConfig config) {
        this.config = config;
    }

    CurseConfig config() {
        return config;
    }

    void reload(CurseConfig config) {
        this.config = config;
    }

    boolean isCursed(String playerName) {
        return CurseTargetMatcher.isTargeted(config.targets, playerName);
    }

    /** Records the moment {@code uuid} was first seen cursed this session, if it hasn't already
     *  been recorded -- idempotent, safe to call every tick. */
    void markCurseStarted(UUID uuid, long nowEpochSecond) {
        curseStartEpochSecond.putIfAbsent(uuid, nowEpochSecond);
    }

    /** Seconds this player has been continuously cursed so far this session, or 0 if they
     *  haven't been marked cursed at all (shouldn't normally happen for a caller that already
     *  confirmed {@link #isCursed}, but never negative/garbage either way). */
    long secondsCursed(UUID uuid, long nowEpochSecond) {
        Long startedAt = curseStartEpochSecond.get(uuid);
        return startedAt == null ? 0 : Math.max(0, nowEpochSecond - startedAt);
    }

    void clearCurseTracking(UUID uuid) {
        curseStartEpochSecond.remove(uuid);
    }

    Double originalMaxHealth(UUID uuid) {
        return originalMaxHealth.get(uuid);
    }

    void rememberOriginalMaxHealth(UUID uuid, double value) {
        originalMaxHealth.putIfAbsent(uuid, value);
    }

    void forgetOriginalMaxHealth(UUID uuid) {
        originalMaxHealth.remove(uuid);
    }
}
