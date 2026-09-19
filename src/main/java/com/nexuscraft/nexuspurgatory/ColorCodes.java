package com.nexuscraft.nexuspurgatory;

/** '&'-shorthand to real section-sign color codes -- same tiny utility every plugin in this
 *  family carries its own copy of, rather than adding a cross-plugin dependency for one method. */
final class ColorCodes {

    private ColorCodes() {
    }

    static String translate(String raw) {
        return raw == null ? null : raw.replace('&', '§');
    }
}
