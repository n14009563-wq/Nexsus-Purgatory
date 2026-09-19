package com.nexuscraft.nexuspurgatory;

/** Pure string formatting: {@code "DIAMOND_SWORD"} -> {@code "Diamond Sword"} -- used only for
 *  the item-drain taunt message's {@code %item%} placeholder, so it reads like a name instead of
 *  a raw enum constant. */
final class MaterialNames {

    private MaterialNames() {
    }

    static String humanize(String materialName) {
        if (materialName == null || materialName.isBlank()) {
            return "item";
        }
        String[] words = materialName.split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                result.append(word.substring(1).toLowerCase());
            }
        }
        return result.toString();
    }
}
