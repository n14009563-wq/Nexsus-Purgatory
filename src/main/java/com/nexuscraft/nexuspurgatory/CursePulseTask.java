package com.nexuscraft.nexuspurgatory;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/** Runs every second and independently decides, per cursed player, whether it's time for their
 *  next "curse pulse" -- a short burst of one random extra debuff (Nausea, Blindness, Mining
 *  Fatigue, or Weakness) layered on top of the constant baseline (health cap, hunger drain,
 *  slowness). This is what keeps the misery from becoming a static, adapted-to baseline, and
 *  (via {@link CurseEscalation}) what actually gets worse the longer someone stays cursed in one
 *  sitting -- the direct mechanism behind "not so fast they rage quit, but absolutely to where
 *  they will." Only fires when {@code config.cursePulseEnabled} is true. */
final class CursePulseTask implements Runnable {

    private static final List<PotionEffectType> PULSE_EFFECTS = List.of(
            PotionEffectType.NAUSEA, PotionEffectType.BLINDNESS,
            PotionEffectType.MINING_FATIGUE, PotionEffectType.WEAKNESS);

    private final CurseState state;
    private final Random random = new Random();
    private final Map<UUID, Long> lastPulseEpochSecond = new HashMap<>();

    CursePulseTask(CurseState state) {
        this.state = state;
    }

    @Override
    public void run() {
        long now = Instant.now().getEpochSecond();
        CurseConfig config = state.config();
        if (!config.cursePulseEnabled) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!CurseTargets.checkAndTrack(state, player, now)) {
                lastPulseEpochSecond.remove(player.getUniqueId());
                continue;
            }
            maybePulse(player, config, now);
        }
    }

    private void maybePulse(Player player, CurseConfig config, long now) {
        UUID uuid = player.getUniqueId();
        int stage = config.escalationEnabled
                ? CurseEscalation.stageFor(state.secondsCursed(uuid, now), config.escalationStageThresholdSeconds)
                : 0;
        int interval = config.escalationEnabled
                ? CurseEscalation.scaleInterval(config.cursePulseIntervalSeconds, stage,
                        config.escalationPulseIntervalReductionPerStageSeconds, config.escalationMinPulseIntervalSeconds)
                : config.cursePulseIntervalSeconds;

        Long last = lastPulseEpochSecond.get(uuid);
        if (last != null && now - last < interval) {
            return;
        }
        lastPulseEpochSecond.put(uuid, now);

        PotionEffectType effect = PULSE_EFFECTS.get(random.nextInt(PULSE_EFFECTS.size()));
        int durationTicks = config.cursePulseDurationSeconds * 20;
        player.addPotionEffect(new PotionEffect(effect, durationTicks, 0));
    }
}
