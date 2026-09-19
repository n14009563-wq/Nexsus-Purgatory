package com.nexuscraft.nexuspurgatory;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Instant;

/** Runs every {@code config.hungerDrainIntervalSeconds} (default 45): drains a cursed player's
 *  hunger and saturation down to almost nothing. Combined with the one-heart health cap from
 *  {@link CurseHealthTask}, sustained low hunger's own natural-regen-block and starvation damage
 *  do the rest -- this task's only job is keeping the hunger bar from ever recovering on its
 *  own. */
final class CurseHungerTask implements Runnable {

    private final CurseState state;

    CurseHungerTask(CurseState state) {
        this.state = state;
    }

    @Override
    public void run() {
        long now = Instant.now().getEpochSecond();
        CurseConfig config = state.config();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (CurseTargets.checkAndTrack(state, player, now)) {
                player.setFoodLevel(config.hungerDrainToLevel);
                player.setSaturation(0f);
            }
        }
    }
}
