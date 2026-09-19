package com.nexuscraft.nexuspurgatory;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.time.Instant;
import java.util.Random;

/** Runs every {@code config.inventoryJumbleIntervalSeconds} (default 45): shuffles a cursed
 *  player's main inventory + hotbar contents into a random order. Armor and off-hand are left
 *  alone -- {@code getStorageContents()}/{@code setStorageContents()} only ever touch the 36
 *  carryable slots, never what's actually worn. The actual shuffle is {@link InventoryShuffler},
 *  pure and unit-tested on its own; this class is just the "grab it, shuffle it, put it back"
 *  glue. */
final class CurseInventoryJumbleTask implements Runnable {

    private final CurseState state;
    private final Random random = new Random();

    CurseInventoryJumbleTask(CurseState state) {
        this.state = state;
    }

    @Override
    public void run() {
        long now = Instant.now().getEpochSecond();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!CurseTargets.checkAndTrack(state, player, now)) {
                continue;
            }
            PlayerInventory inventory = player.getInventory();
            ItemStack[] shuffled = InventoryShuffler.shuffle(inventory.getStorageContents(), random);
            inventory.setStorageContents(shuffled);
        }
    }
}
