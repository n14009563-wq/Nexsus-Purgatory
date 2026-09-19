package com.nexuscraft.nexuspurgatory;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/** Clears a departing player's per-session escalation clock and tears down their boss bar the
 *  instant they log off -- so escalation for someone who quits and rejoins later starts back at
 *  the base stage rather than picking up where the previous session left off (a deliberate,
 *  documented v1 scope choice -- see README's "What this deliberately doesn't do"), and no boss
 *  bar is ever left dangling for a player who's no longer online to see it.
 *
 *  <p>Deliberately does NOT touch {@code originalMaxHealth}: a player who logs off still cursed
 *  stays capped in their own saved player data (Bukkit persists the max-health attribute like any
 *  other), and {@link CurseHealthTask} settles it fresh the moment they're next seen online --
 *  still cursed (recaps them, matching "zero mercy -- the curse is active the instant they
 *  respawn/rejoin") or finally released (restores their original max health then, not before). */
final class CurseSessionListener implements Listener {

    private final CurseState state;
    private final CurseBossBarManager bossBarManager;

    CurseSessionListener(CurseState state, CurseBossBarManager bossBarManager) {
        this.state = state;
        this.bossBarManager = bossBarManager;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        state.clearCurseTracking(player.getUniqueId());
        bossBarManager.remove(player.getUniqueId());
    }
}
