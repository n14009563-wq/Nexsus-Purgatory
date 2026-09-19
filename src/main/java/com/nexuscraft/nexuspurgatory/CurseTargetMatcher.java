package com.nexuscraft.nexuspurgatory;

import java.util.List;

/** The single place that decides whether a given in-game name is currently one of this plugin's
 *  configured targets -- matched case-insensitively against either the Java username or the Xbox
 *  gamertag field (for a Floodgate/Bedrock connection, {@code Player#getName()} already IS the
 *  Xbox gamertag, Floodgate presents it transparently, so no separate Bedrock-detection code is
 *  needed here -- same "Player#getName() already works for both" observation NexusPrison's own
 *  {@code PrisonerMatcher} relies on).
 *
 *  <p>Deliberately re-evaluated live against the current config every time, rather than resolved
 *  once and cached by UUID -- unlike NexusPrison's confinement (a sentence that must survive a
 *  config edit by design), there is no reason this plugin's effects shouldn't stop the instant an
 *  admin removes a name from {@code config.yml} and runs {@code /nexuspurgatory reload}. That's
 *  this plugin's whole escape hatch: no separate "pardon" command exists because none is needed. */
final class CurseTargetMatcher {

    private CurseTargetMatcher() {
    }

    static boolean isTargeted(List<CurseTargetConfig> targets, String playerName) {
        if (playerName == null || playerName.isBlank() || targets == null) {
            return false;
        }
        for (CurseTargetConfig target : targets) {
            if (playerName.equalsIgnoreCase(target.javaUsername()) || playerName.equalsIgnoreCase(target.xboxGamertag())) {
                return true;
            }
        }
        return false;
    }
}
