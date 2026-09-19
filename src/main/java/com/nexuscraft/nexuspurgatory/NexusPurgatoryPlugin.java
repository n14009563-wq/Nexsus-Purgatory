package com.nexuscraft.nexuspurgatory;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/** Wires the whole plugin together. Every effect -- health cap, hunger drain, guaranteed-lethal
 *  mob damage, slowness, inventory jumble, item drain, escalating curse-pulses, the boss bar --
 *  ultimately answers to the exact same question, asked fresh every single tick: {@link
 *  CurseTargetMatcher#isTargeted}, checked against whatever config.yml currently says. Nobody
 *  whose name isn't listed there is ever touched, by any task, for any reason -- that's not an
 *  incidental property of this code, it's the one requirement everything else was built around.
 *
 *  <p>Health/curse-pulse/boss-bar run every second regardless of config, since each of those
 *  either needs per-tick precision (the health cap must stick immediately against any regen
 *  source) or does its own internal per-player interval bookkeeping ({@link CursePulseTask}).
 *  Hunger, slowness, inventory-jumble, and item-drain instead run on their own configured
 *  interval directly -- see {@code CurseConfig} for each default. Changing one of those interval
 *  values in config.yml needs a server restart to take effect (Bukkit doesn't support
 *  re-periodizing a live scheduled task); {@code /nexuspurgatory reload} still immediately applies
 *  every other change, most importantly the {@code targets} list itself. */
public final class NexusPurgatoryPlugin extends JavaPlugin {

    private CurseState state;
    private CurseBossBarManager bossBarManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        CurseConfig config = new CurseConfig(this);
        config.load();
        this.state = new CurseState(config);
        this.bossBarManager = new CurseBossBarManager();

        getServer().getPluginManager().registerEvents(new CurseMobDamageListener(state), this);
        getServer().getPluginManager().registerEvents(new CurseSessionListener(state, bossBarManager), this);

        getCommand("nexuspurgatory").setExecutor(new CurseCommand(state, this::reload));

        Bukkit.getScheduler().runTaskTimer(this, new CurseHealthTask(state), 20L, 20L);
        Bukkit.getScheduler().runTaskTimer(this, new CurseHungerTask(state),
                config.hungerDrainIntervalSeconds * 20L, config.hungerDrainIntervalSeconds * 20L);
        Bukkit.getScheduler().runTaskTimer(this, new CurseSlownessTask(state),
                config.slownessRefreshIntervalSeconds * 20L, config.slownessRefreshIntervalSeconds * 20L);
        Bukkit.getScheduler().runTaskTimer(this, new CurseInventoryJumbleTask(state),
                config.inventoryJumbleIntervalSeconds * 20L, config.inventoryJumbleIntervalSeconds * 20L);
        Bukkit.getScheduler().runTaskTimer(this, new CurseItemDrainTask(state),
                config.itemDrainIntervalSeconds * 20L, config.itemDrainIntervalSeconds * 20L);
        Bukkit.getScheduler().runTaskTimer(this, new CursePulseTask(state), 20L, 20L);
        Bukkit.getScheduler().runTaskTimer(this, new CurseBossBarTask(state, bossBarManager), 20L, 20L);

        getLogger().info("NexusPurgatory enabled -- " + config.targets.size() + " target(s) configured.");
    }

    @Override
    public void onDisable() {
        if (bossBarManager != null) {
            bossBarManager.removeAll();
        }
        getLogger().info("NexusPurgatory disabled.");
    }

    private void reload() {
        reloadConfig();
        CurseConfig fresh = new CurseConfig(this);
        fresh.load();
        state.reload(fresh);
    }
}
