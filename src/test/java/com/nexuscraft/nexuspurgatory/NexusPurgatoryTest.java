package com.nexuscraft.nexuspurgatory;

import org.bukkit.enchantments.Enchantment;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Standalone test suite (no JUnit -- same house pattern as the rest of the Nexus plugin family):
 * exercises every class in this plugin that's pure logic. Bukkit glue (scheduler tasks, event
 * listeners, the boss bar manager) is NOT exercised here -- it's covered by the -Xlint:all
 * -Werror compile check against the stub API instead, same division of labor this whole Nexus
 * ecosystem uses. See README's "if anything fails to compile" section.
 */
public final class NexusPurgatoryTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        testCurseTargetConfig();
        testCurseTargetMatcher();
        testItemValueHeuristic();
        testInventoryShuffler();
        testCurseEscalation();
        testMaterialNames();

        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed.");
        if (failed > 0) {
            System.exit(1);
        }
    }

    // --- CurseTargetConfig ---

    private static void testCurseTargetConfig() {
        section("CurseTargetConfig");

        CurseTargetConfig javaOnly = new CurseTargetConfig("Steve", null);
        check("accepts a javaUsername-only entry", javaOnly.javaUsername().equals("Steve"));

        CurseTargetConfig xboxOnly = new CurseTargetConfig(null, "SteveXBL");
        check("accepts an xboxGamertag-only entry", xboxOnly.xboxGamertag().equals("SteveXBL"));

        CurseTargetConfig both = new CurseTargetConfig("Steve", "SteveXBL");
        check("accepts an entry with both set", both.javaUsername().equals("Steve") && both.xboxGamertag().equals("SteveXBL"));

        try {
            new CurseTargetConfig(null, null);
            check("rejects an entry with neither identity set", false);
        } catch (IllegalArgumentException expected) {
            check("rejects an entry with neither identity set", true);
        }

        try {
            new CurseTargetConfig("   ", "");
            check("rejects an entry where both fields are blank", false);
        } catch (IllegalArgumentException expected) {
            check("rejects an entry where both fields are blank", true);
        }

        CurseTargetConfig blankJavaRealXbox = new CurseTargetConfig("   ", "SteveXBL");
        check("a blank javaUsername with a real xboxGamertag is still accepted", blankJavaRealXbox.xboxGamertag().equals("SteveXBL"));
    }

    // --- CurseTargetMatcher ---

    private static void testCurseTargetMatcher() {
        section("CurseTargetMatcher");

        List<CurseTargetConfig> targets = List.of(
                new CurseTargetConfig("Steve", null),
                new CurseTargetConfig(null, "AlexXBL"),
                new CurseTargetConfig("Herobrine", "HerobrineXBL"));

        check("matches an exact javaUsername", CurseTargetMatcher.isTargeted(targets, "Steve"));
        check("matches a javaUsername case-insensitively", CurseTargetMatcher.isTargeted(targets, "sTeVe"));
        check("matches an exact xboxGamertag", CurseTargetMatcher.isTargeted(targets, "AlexXBL"));
        check("matches an xboxGamertag case-insensitively", CurseTargetMatcher.isTargeted(targets, "alexxbl"));
        check("matches either field on a dual entry", CurseTargetMatcher.isTargeted(targets, "HerobrineXBL"));
        check("does not match a name not on the list", !CurseTargetMatcher.isTargeted(targets, "RandomPlayer"));
        check("does not match null", !CurseTargetMatcher.isTargeted(targets, null));
        check("does not match a blank name", !CurseTargetMatcher.isTargeted(targets, "   "));
        check("an empty target list matches nobody", !CurseTargetMatcher.isTargeted(List.of(), "Steve"));
        check("a null target list matches nobody rather than throwing", !CurseTargetMatcher.isTargeted(null, "Steve"));
    }

    // --- ItemValueHeuristic ---

    private static void testItemValueHeuristic() {
        section("ItemValueHeuristic");

        check("netherite outranks diamond", ItemValueHeuristic.score("NETHERITE_SWORD", Map.of())
                > ItemValueHeuristic.score("DIAMOND_SWORD", Map.of()));
        check("diamond outranks iron", ItemValueHeuristic.score("DIAMOND_PICKAXE", Map.of())
                > ItemValueHeuristic.score("IRON_PICKAXE", Map.of()));
        check("iron outranks wood", ItemValueHeuristic.score("IRON_AXE", Map.of())
                > ItemValueHeuristic.score("WOODEN_AXE", Map.of()));
        check("an unrecognized material still scores a small positive baseline", ItemValueHeuristic.score("DIRT", Map.of()) > 0);
        check("a null material name scores zero", ItemValueHeuristic.score(null, Map.of()) == 0);

        int plainDiamond = ItemValueHeuristic.score("DIAMOND_SWORD", Map.of());
        int enchantedDiamond = ItemValueHeuristic.score("DIAMOND_SWORD", Map.of(Enchantment.FORTUNE, 3));
        check("enchantments add on top of the base material score", enchantedDiamond > plainDiamond);

        int oneEnchant = ItemValueHeuristic.score("STONE_SWORD", Map.of(Enchantment.FORTUNE, 1));
        int higherLevelEnchant = ItemValueHeuristic.score("STONE_SWORD", Map.of(Enchantment.FORTUNE, 5));
        check("a higher enchantment level scores higher", higherLevelEnchant > oneEnchant);

        check("null enchantment map is treated as none, not a crash", ItemValueHeuristic.score("STONE_SWORD", null) > 0);
        check("totem of undying scores very high", ItemValueHeuristic.score("TOTEM_OF_UNDYING", Map.of()) > 300);
        check("an enchanted book counts as valuable on its own", ItemValueHeuristic.score("ENCHANTED_BOOK", Map.of()) > 200);
    }

    // --- InventoryShuffler ---

    private static void testInventoryShuffler() {
        section("InventoryShuffler");

        Random fixedSeed = new Random(42);
        String[] original = {"A", "B", "C", "D", "E", "F", "G", "H"};
        String[] originalCopyForComparison = original.clone();
        String[] shuffled = InventoryShuffler.shuffle(original, fixedSeed);

        check("returns an array of the same length", shuffled.length == original.length);
        check("does not mutate the input array", java.util.Arrays.equals(original, originalCopyForComparison));
        check("the shuffled array contains exactly the same elements", sameMultiset(original, shuffled));

        String[] single = {"OnlyOne"};
        String[] shuffledSingle = InventoryShuffler.shuffle(single, new Random());
        check("a single-element array shuffles to itself", java.util.Arrays.equals(single, shuffledSingle));

        String[] empty = {};
        String[] shuffledEmpty = InventoryShuffler.shuffle(empty, new Random());
        check("an empty array shuffles to an empty array", shuffledEmpty.length == 0);

        Integer[] withNulls = {1, null, 3, null, 5};
        Integer[] shuffledWithNulls = InventoryShuffler.shuffle(withNulls, new Random(7));
        int nullCount = 0;
        for (Integer i : shuffledWithNulls) {
            if (i == null) {
                nullCount++;
            }
        }
        check("preserves null entries (empty inventory slots) through the shuffle", nullCount == 2);

        boolean sawADifferentOrder = false;
        for (int attempt = 0; attempt < 20; attempt++) {
            String[] source = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J"};
            String[] result = InventoryShuffler.shuffle(source, new Random(attempt));
            if (!java.util.Arrays.equals(source, result)) {
                sawADifferentOrder = true;
                break;
            }
        }
        check("actually reorders elements at least some of the time", sawADifferentOrder);
    }

    private static boolean sameMultiset(String[] a, String[] b) {
        String[] sortedA = a.clone();
        String[] sortedB = b.clone();
        java.util.Arrays.sort(sortedA);
        java.util.Arrays.sort(sortedB);
        return java.util.Arrays.equals(sortedA, sortedB);
    }

    // --- CurseEscalation ---

    private static void testCurseEscalation() {
        section("CurseEscalation");

        List<Long> thresholds = List.of(300L, 900L, 1800L); // 5m, 15m, 30m

        checkEquals("stage 0 before the first threshold", CurseEscalation.stageFor(0, thresholds), 0);
        checkEquals("stage 0 just before the first threshold", CurseEscalation.stageFor(299, thresholds), 0);
        checkEquals("stage 1 exactly at the first threshold", CurseEscalation.stageFor(300, thresholds), 1);
        checkEquals("stage 1 between the first and second threshold", CurseEscalation.stageFor(500, thresholds), 1);
        checkEquals("stage 2 exactly at the second threshold", CurseEscalation.stageFor(900, thresholds), 2);
        checkEquals("stage 3 at or past the final threshold", CurseEscalation.stageFor(1800, thresholds), 3);
        checkEquals("stage caps at the number of thresholds, however long", CurseEscalation.stageFor(999999, thresholds), 3);
        checkEquals("an empty threshold list is always stage 0", CurseEscalation.stageFor(999999, List.of()), 0);

        checkEquals("stage 0 keeps the base interval", CurseEscalation.scaleInterval(90, 0, 15, 20), 90);
        checkEquals("each stage reduces the interval", CurseEscalation.scaleInterval(90, 1, 15, 20), 75);
        checkEquals("stage 2 reduces further", CurseEscalation.scaleInterval(90, 2, 15, 20), 60);
        checkEquals("never drops below the configured floor", CurseEscalation.scaleInterval(90, 10, 15, 20), 20);
        checkEquals("floor applies even at a moderate stage that would otherwise go negative",
                CurseEscalation.scaleInterval(30, 3, 15, 20), 20);
    }

    // --- MaterialNames ---

    private static void testMaterialNames() {
        section("MaterialNames");

        checkEquals("splits and title-cases a two-word material", MaterialNames.humanize("DIAMOND_SWORD"), "Diamond Sword");
        checkEquals("handles a single-word material", MaterialNames.humanize("TOTEM"), "Totem");
        checkEquals("handles a three-word material", MaterialNames.humanize("NETHERITE_HORSE_ARMOR"), "Netherite Horse Armor");
        checkEquals("a null material name falls back to a generic label", MaterialNames.humanize(null), "item");
        checkEquals("a blank material name falls back to a generic label", MaterialNames.humanize("   "), "item");
        checkEquals("collapses a stray double underscore without blowing up", MaterialNames.humanize("GOLD__NUGGET"), "Gold Nugget");
    }

    // --- helpers ---

    private static void section(String name) {
        System.out.println("-- " + name + " --");
    }

    private static void check(String description, boolean condition) {
        if (condition) {
            passed++;
        } else {
            failed++;
            System.out.println("  FAIL: " + description);
        }
    }

    private static void checkEquals(String description, Object actual, Object expected) {
        check(description + " (expected " + expected + ", got " + actual + ")",
                actual == null ? expected == null : actual.equals(expected));
    }
}
