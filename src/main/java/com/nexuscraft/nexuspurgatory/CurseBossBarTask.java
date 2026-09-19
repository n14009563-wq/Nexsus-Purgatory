package com.nexuscraft.nexuspurgatory;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.List;

/** Runs every second: keeps a boss bar over each cursed player's own screen -- title shows how
 *  long they've been cursed this session (and their current escalation stage, once past the base
 *  one), progress fills toward 1.0 as they approach the final configured escalation stage. It
 *  isn't information the player can act on (there's no pardon command to run, by design -- see
 *  {@link CurseTargetMatcher}), it's a constant, impossible-to-mistake-for-a-glitch reminder that
 *  this isn't stopping, which is what actually wears a player down over a long session rather
 *  than a one-off jolt they shrug off. Skips (and tears down) a player's bar the instant they
 *  stop being cursed, or entirely whenever {@code config.bossBarEnabled} is false. */
final class CurseBossBarTask implements Runnable {

    private final CurseState state;
    private final CurseBossBarManager bossBarManager;

    CurseBossBarTask(CurseState state, CurseBossBarManager bossBarManager) {
        this.state = state;
        this.bossBarManager = bossBarManager;
    }

    @Override
    public void run() {
        long now = Instant.now().getEpochSecond();
        CurseConfig config = state.config();
        for (Player player : Bukkit.getOnlinePlayers()) {
            boolean cursed = CurseTargets.checkAndTrack(state, player, now);
            if (!cursed || !config.bossBarEnabled) {
                bossBarManager.remove(player.getUniqueId());
                continue;
            }

            long secondsCursed = state.secondsCursed(player.getUniqueId(), now);
            int stage = config.escalationEnabled
                    ? CurseEscalation.stageFor(secondsCursed, config.escalationStageThresholdSeconds)
                    : 0;
            String title = ColorCodes.translate("&4&lCURSED &7- &f" + formatDuration(secondsCursed)
                    + (stage > 0 ? " &7(stage " + stage + ")" : ""));
            double progress = progressFor(secondsCursed, config);

            bossBarManager.update(player, title, progress, config.resolveBossBarColor(), config.resolveBossBarStyle());
        }
    }

    /** Fills toward 1.0 as a player approaches the final escalation stage's threshold -- a
     *  constant full bar when escalation is disabled or has no thresholds configured, since
     *  there's then nothing to visually count up toward. */
    private static double progressFor(long secondsCursed, CurseConfig config) {
        List<Long> thresholds = config.escalationStageThresholdSeconds;
        if (!config.escalationEnabled || thresholds == null || thresholds.isEmpty()) {
            return 1.0;
        }
        long finalThreshold = thresholds.get(thresholds.size() - 1);
        if (finalThreshold <= 0) {
            return 1.0;
        }
        return Math.max(0.0, Math.min(1.0, secondsCursed / (double) finalThreshold));
    }

    private static String formatDuration(long totalSeconds) {
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return minutes + "m " + seconds + "s";
    }
}
