package com.ben.csp.rbmk.physics;

/**
 * Delayed-neutron data: the six groups that give a reactor its controllable
 * period. These are the numbers every reactor calculation starts from, and the
 * RBMK is no different — the accident is, at bottom, a story about delayed
 * neutrons (a reactivity insertion comparable to beta).
 *
 * <p>Group decay constants and relative abundances are the standard six-group
 * (Keepin) set for thermal fission of U-235. beta_eff for the RBMK-1000 is taken
 * as 0.0065 (the value the RBMK literature quotes); mark/verify against a NIKIET
 * source before the physics is frozen.
 *
 * <p>These are NOT frozen facts: each is registered in {@link PhysicsProvenance}
 * under {@code delayed.*} as {@code to_verify}, so the build flags them until a
 * primary source replaces them.
 */
public final class DelayedNeutrons {

    private DelayedNeutrons() {}

    public static final int GROUPS = 6;

    /** Effective delayed-neutron fraction, beta_eff. RBMK-1000 ~ 0.0065. */
    public static final double BETA_EFFECTIVE = 0.0065;

    /** Group decay constants lambda_i, s^-1 (Keepin, six-group). */
    public static final double[] LAMBDA = {0.0124, 0.0305, 0.111, 0.301, 1.14, 3.01};

    /** Group abundances beta_i/beta (Keepin, six-group); sums to 1. */
    public static final double[] BETA_FRACTION = {0.038, 0.213, 0.188, 0.407, 0.128, 0.026};

    /** Prompt-neutron generation time Lambda, seconds. RBMK-1000 ~ 0.7 ms. TO VERIFY. */
    public static final double GENERATION_TIME = 0.0007;

    public static double beta(int i) {
        return BETA_EFFECTIVE * BETA_FRACTION[i];
    }

    /** Sum of the group fractions — must be 1 for the data to be consistent. */
    public static double fractionSum() {
        double s = 0;
        for (double f : BETA_FRACTION) {
            s += f;
        }
        return s;
    }
}
