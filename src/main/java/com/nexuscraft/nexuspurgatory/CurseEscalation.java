package com.nexuscraft.nexuspurgatory;

import java.util.List;

/** Pure "how much worse should this get, given how long they've been suffering" math -- the
 *  direct answer to "not so fast that they rage quit, but absolutely to where they will": a
 *  freshly-cursed player gets the base severity in config.yml, and only someone who stays online
 *  and cursed for a while starts seeing curse-pulses (see {@code CursePulseTask}) land more
 *  often. Escalation is tracked per continuous online session (see {@code CurseState}), not
 *  persisted -- logging off and back on does reset it; see README for why that's a deliberate v1
 *  scope choice, not an oversight. */
final class CurseEscalation {

    private CurseEscalation() {
    }

    /** {@code thresholdSeconds} must already be sorted ascending. Returns how many thresholds
     *  {@code secondsCursed} has passed -- 0 means "still in the base stage." */
    static int stageFor(long secondsCursed, List<Long> thresholdSeconds) {
        int stage = 0;
        for (long threshold : thresholdSeconds) {
            if (secondsCursed >= threshold) {
                stage++;
            } else {
                break;
            }
        }
        return stage;
    }

    /** Shrinks an interval by a fixed amount per escalation stage, floored at {@code
     *  minSeconds} -- used to make curse-pulses (and potentially other periodic effects) land
     *  more often the longer someone has been cursed, without ever going below a sane floor. */
    static int scaleInterval(int baseSeconds, int stage, int reductionPerStageSeconds, int minSeconds) {
        int scaled = baseSeconds - stage * reductionPerStageSeconds;
        return Math.max(minSeconds, scaled);
    }
}
