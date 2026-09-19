package com.nexuscraft.nexuspurgatory;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.UUID;

/** Runs every second: caps a cursed player's max health down to {@code
 *  config.capHealthPoints} (real Bukkit clamps current health to max automatically, so this
 *  alone is what makes the one-heart cap "stick" against any regen source -- natural, potion, or
 *  a golden apple) and restores a no-longer-cursed player's original max health the instant
 *  they're released (admin edit + reload, or logging off and no longer matching). Deliberately
 *  does NOT touch current health upward -- a cursed player who's clawed their way to exactly the
 *  cap keeps whatever health they have, this task only ever pulls the ceiling down (or, on
 *  release, back up) to meet it. */
final class CurseHealthTask implements Runnable {

    private final CurseState state;

    CurseHealthTask(CurseState state) {
        this.state = state;
    }

    @Override
    public void run() {
        long now = Instant.now().getEpochSecond();
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            boolean cursed = CurseTargets.checkAndTrack(state, player, now);
            AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (maxHealth == null) {
                continue;
            }
            if (cursed) {
                state.rememberOriginalMaxHealth(uuid, maxHealth.getBaseValue());
                maxHealth.setBaseValue(state.config().capHealthPoints);
                if (player.getHealth() > maxHealth.getValue()) {
                    player.setHealth(maxHealth.getValue());
                }
            } else {
                Double original = state.originalMaxHealth(uuid);
                if (original != null) {
                    maxHealth.setBaseValue(original);
                    state.forgetOriginalMaxHealth(uuid);
                }
            }
        }
    }
}
