package com.nexuscraft.nexuspurgatory;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** One {@link BossBar} per cursed player, shown only to them -- a quiet, constant "this isn't
 *  stopping" indicator (elapsed time cursed this session, and escalation stage once it's past
 *  the base one). Bars are created lazily on first need and torn down the instant a player stops
 *  being cursed or logs off, never left dangling. */
final class CurseBossBarManager {

    private final Map<UUID, BossBar> bars = new ConcurrentHashMap<>();

    void update(Player player, String title, double progress, BarColor color, BarStyle style) {
        BossBar bar = bars.computeIfAbsent(player.getUniqueId(), id -> {
            BossBar created = Bukkit.createBossBar(title, color, style);
            created.addPlayer(player);
            return created;
        });
        bar.setTitle(title);
        bar.setProgress(progress);
        bar.setColor(color);
        bar.setStyle(style);
    }

    void remove(UUID uuid) {
        BossBar bar = bars.remove(uuid);
        if (bar != null) {
            bar.removeAll();
            bar.setVisible(false);
        }
    }

    void removeAll() {
        for (UUID uuid : Map.copyOf(bars).keySet()) {
            remove(uuid);
        }
    }
}
