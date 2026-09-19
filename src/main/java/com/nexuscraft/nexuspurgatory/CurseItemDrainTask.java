package com.nexuscraft.nexuspurgatory;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.time.Instant;

/** Runs every {@code config.itemDrainIntervalSeconds} (default 300 -- five minutes): finds the
 *  single worst item to lose in a cursed player's inventory (see {@link ItemValueHeuristic}) and
 *  deletes it outright, with a taunting chat message. Only ever removes ONE item per cycle, and
 *  only from the 36 carryable storage slots -- worn armor is never touched by this effect. */
final class CurseItemDrainTask implements Runnable {

    private final CurseState state;

    CurseItemDrainTask(CurseState state) {
        this.state = state;
    }

    @Override
    public void run() {
        long now = Instant.now().getEpochSecond();
        CurseConfig config = state.config();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!CurseTargets.checkAndTrack(state, player, now)) {
                continue;
            }
            drainWorstItem(player, config);
        }
    }

    private void drainWorstItem(Player player, CurseConfig config) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getStorageContents();

        int worstIndex = -1;
        int worstScore = -1;
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (stack == null || stack.getType() == null) {
                continue;
            }
            int score = ItemValueHeuristic.score(stack.getType().name(), stack.getEnchantments());
            if (score > worstScore) {
                worstScore = score;
                worstIndex = i;
            }
        }

        if (worstIndex < 0) {
            return; // empty inventory -- nothing to drain this cycle.
        }

        String itemName = MaterialNames.humanize(contents[worstIndex].getType().name());
        inventory.setItem(worstIndex, null);
        String message = config.itemDrainTauntMessage.replace("%item%", itemName);
        player.sendMessage(ColorCodes.translate(message));
    }
}
