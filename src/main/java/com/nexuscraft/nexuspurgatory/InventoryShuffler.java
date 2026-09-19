package com.nexuscraft.nexuspurgatory;

import java.util.Random;

/** Pure Fisher-Yates shuffle, generic over the array's element type on purpose -- so the "does
 *  this actually produce a random permutation that keeps the exact same elements" question can
 *  be unit-tested with plain {@code String[]}/{@code Integer[]} fixtures, with zero dependency on
 *  {@code ItemStack} or any other Bukkit type. The Bukkit-glue call site ({@code
 *  CurseInventoryJumbleTask}) is the only place this ever actually touches a player's real
 *  inventory contents. */
final class InventoryShuffler {

    private InventoryShuffler() {
    }

    /** Shuffles a COPY of {@code array} and returns it -- the input array is never mutated in
     *  place, so a caller holding a live reference to it (e.g. straight out of {@code
     *  Inventory#getStorageContents()}, which real Bukkit documents as already being a copy, but
     *  a stub or a future refactor might not) never sees it change out from under them. */
    static <T> T[] shuffle(T[] array, Random random) {
        T[] copy = array.clone();
        for (int i = copy.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            T temp = copy[i];
            copy[i] = copy[j];
            copy[j] = temp;
        }
        return copy;
    }
}
