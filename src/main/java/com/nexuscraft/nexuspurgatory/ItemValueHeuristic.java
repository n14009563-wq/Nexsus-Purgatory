package com.nexuscraft.nexuspurgatory;

import org.bukkit.enchantments.Enchantment;

import java.util.Map;

/** Pure scoring logic for "how much would it hurt to lose this item" -- used by the item-drain
 *  effect to pick the single worst item to delete out of a cursed player's inventory each cycle.
 *  Matches by {@code Material} NAME pattern rather than an exhaustive enum switch, on purpose:
 *  real Paper's {@code Material} enum has hundreds of entries this plugin family's stub doesn't
 *  (and shouldn't try to) model one-by-one, and a name-pattern match is exactly as correct
 *  against the real jar as it is here. */
final class ItemValueHeuristic {

    private ItemValueHeuristic() {
    }

    static int score(String materialName, Map<Enchantment, Integer> enchantments) {
        if (materialName == null) {
            return 0;
        }
        int score = materialTierScore(materialName);
        if (enchantments != null) {
            for (int level : enchantments.values()) {
                score += 20 + level * 15;
            }
        }
        return score;
    }

    private static int materialTierScore(String name) {
        if (name.contains("NETHERITE")) {
            return 400;
        }
        if (name.contains("TOTEM_OF_UNDYING")) {
            return 380;
        }
        if (name.contains("NETHER_STAR")) {
            return 350;
        }
        if (name.contains("ELYTRA")) {
            return 340;
        }
        if (name.contains("DIAMOND")) {
            return 300;
        }
        if (name.contains("EMERALD")) {
            return 260;
        }
        if (name.contains("ENCHANTED_BOOK")) {
            return 220;
        }
        if (name.contains("GOLD")) {
            return 150;
        }
        if (name.contains("IRON")) {
            return 100;
        }
        if (name.contains("COPPER") || name.contains("STONE")) {
            return 40;
        }
        if (name.contains("WOOD") || name.contains("WOODEN")) {
            return 20;
        }
        if (name.contains("LEATHER")) {
            return 15;
        }
        // Deliberately a small positive baseline, not 0 -- a cursed player whose entire
        // inventory is "worthless" junk still loses SOMETHING each cycle rather than the effect
        // going quietly inert just because nothing in their bag matched a known tier keyword.
        return 10;
    }
}
