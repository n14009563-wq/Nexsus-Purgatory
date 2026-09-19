package com.nexuscraft.nexuspurgatory;

/** One line from config.yml's {@code targets} list -- a real person's identity, as the admin
 *  typed it in. At least one of {@code javaUsername}/{@code xboxGamertag} must be set. This is
 *  the ONLY way a player is ever selected for this plugin's effects -- see {@link
 *  CurseTargetMatcher}, which is the single place that decides "is this online player cursed." */
public record CurseTargetConfig(String javaUsername, String xboxGamertag) {

    public CurseTargetConfig {
        boolean hasJava = javaUsername != null && !javaUsername.isBlank();
        boolean hasXbox = xboxGamertag != null && !xboxGamertag.isBlank();
        if (!hasJava && !hasXbox) {
            throw new IllegalArgumentException("a target entry needs a javaUsername and/or an xboxGamertag");
        }
    }
}
