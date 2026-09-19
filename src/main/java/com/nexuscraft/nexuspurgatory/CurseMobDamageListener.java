package com.nexuscraft.nexuspurgatory;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Random;

/** Amplifies mob-inflicted damage against a cursed player so it's near-guaranteed (or, at the
 *  default {@code mobDamage.lethalityChance: 1.0}, absolutely guaranteed) lethal -- "if they get
 *  attacked by mobs, it almost or absolutely kills them every single time," as asked for.
 *  Combined with the one-heart health cap ({@link CurseHealthTask}), most mob hits are already
 *  fatal on their own; this listener's real job is closing the gap for the occasional low-roll
 *  hit that otherwise wouldn't quite finish the job, and giving the admin an explicit dial
 *  ({@code mobDamage.lethalityChance}) rather than leaving it purely to vanilla damage rolls. */
final class CurseMobDamageListener implements Listener {

    private final CurseState state;
    private final Random random = new Random();

    CurseMobDamageListener(CurseState state) {
        this.state = state;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!state.isCursed(player.getName())) {
            return;
        }
        if (!isFromHostileMob(event.getDamager())) {
            return;
        }
        CurseConfig config = state.config();
        if (random.nextDouble() >= config.mobDamageLethalityChance) {
            return;
        }
        double lethalDamage = Math.max(event.getDamage(), player.getHealth());
        event.setDamage(lethalDamage);
    }

    /** True for a direct hit from a hostile mob, or a projectile (an arrow, say) whose shooter
     *  resolves back to one -- a skeleton's arrow is every bit as much "a mob attack" as a
     *  zombie's melee swing. */
    private static boolean isFromHostileMob(Entity damager) {
        if (damager instanceof Monster) {
            return true;
        }
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            return shooter instanceof Monster;
        }
        return false;
    }
}
