package com.nexuscraft.nexuspurgatory;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Instant;

/** Runs every {@code config.slownessRefreshIntervalSeconds} (default 5): re-applies a low-
 *  amplifier Slowness effect to every cursed player, with a duration comfortably longer than the
 *  refresh interval so it never has a chance to actually expire and visibly flicker off between
 *  reapplications. This is the plugin's one "constant, low-grade" effect, deliberately separate
 *  from {@code CursePulseTask}'s intermittent, more severe, randomized effects. */
final class CurseSlownessTask implements Runnable {

    private final CurseState state;

    CurseSlownessTask(CurseState state) {
        this.state = state;
    }

    @Override
    public void run() {
        long now = Instant.now().getEpochSecond();
        CurseConfig config = state.config();
        int durationTicks = (config.slownessRefreshIntervalSeconds + 5) * 20;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (CurseTargets.checkAndTrack(state, player, now)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, durationTicks, config.slownessAmplifier));
            }
        }
    }
}
