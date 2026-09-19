package com.nexuscraft.nexuspurgatory;

import org.bukkit.entity.Player;

/** The one call every periodic task makes first: "is this player cursed right now, and update
 *  {@link CurseState}'s per-session bookkeeping to match." Centralizing it here means every task
 *  (health, hunger, slowness, jumble, item-drain, curse-pulse, boss bar) agrees on the exact same
 *  answer and keeps {@code curseStartEpochSecond} consistent no matter which task happens to run
 *  first in a given tick -- rather than each task re-implementing "check target list, then
 *  remember/forget the start time" slightly differently. */
final class CurseTargets {

    private CurseTargets() {
    }

    static boolean checkAndTrack(CurseState state, Player player, long nowEpochSecond) {
        boolean cursed = state.isCursed(player.getName());
        if (cursed) {
            state.markCurseStarted(player.getUniqueId(), nowEpochSecond);
        } else {
            state.clearCurseTracking(player.getUniqueId());
        }
        return cursed;
    }
}
