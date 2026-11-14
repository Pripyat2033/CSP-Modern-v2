package com.ben.csp.rbmk.physics;

/**
 * Fission-product decay heat: the heat a reactor keeps making after the chain
 * reaction has stopped. This is the part of the physics that turns "the reactor is
 * off" into "the reactor is still hot", and it is why a scrammed RBMK still boils
 * water — the heat that drove the Chernobyl steam explosion was decay heat, not
 * fission.
 *
 * <p>The model is the standard multi-group form
 * <pre>
 *   dH_i/dt = lambda_i * (c_i * P - H_i)
 * </pre>
 * where {@code P} is the fission power (1.0 = the reference critical state) and
 * {@code c_i} is group {@code i}'s share of nominal power at equilibrium. At steady
 * state each group sits at {@code c_i * P}, so the total is
 * {@link #EQUILIBRIUM} of nominal power; after a scram {@code P -> 0} and each group
 * decays as {@code exp(-lambda_i t)}, reproducing the Way-Wigner curve
 * {@code H(t)/P_nominal = EQUILIBRIUM * t^-0.2} (t in seconds, long operation).
 *
 * <p>The {@code lambda_i, c_i} pairs are a least-squares fit of that Way-Wigner
 * curve over 1 s .. 1 day, with {@code sum c_i} fixed to {@link #EQUILIBRIUM}. The
 * fit is in {@code DecayHeatSelfTest}; the constants are registered in
 * {@link PhysicsProvenance} as {@code to_verify} pending a NIKIET/SKALA source.
 */
public final class DecayHeat {

    /**
     * Equilibrium decay heat as a fraction of nominal thermal power. ~6.5 % is the
     * standard figure for a thermal reactor after long operation (Way-Wigner,
     * {@code 0.066 t^-0.2}); it is what a scrammed RBMK sits at.
     */
    public static final double EQUILIBRIUM = 0.065;

    /**
     * Fission power's share of nominal total power at the reference state. The
     * remaining {@link #EQUILIBRIUM} is decay heat, so that at steady state the
     * TOTAL is exactly 1.0 (nominal). Reported power is
     * {@code FISSION_SHARE * P + decayHeat}, which keeps the plant's heat balance
     * unchanged at nominal while giving the correct post-scram plateau.
     */
    public static final double FISSION_SHARE = 1.0 - EQUILIBRIUM;

    /** Group decay constants, 1/s (fit of the Way-Wigner curve). */
    private static final double[] LAMBDA = {
        1.0e-6, 3.879977e-6, 1.505423e-5, 5.841005e-5, 2.266297e-4,
        8.793181e-4, 3.411735e-3, 1.323745e-2, 5.136102e-2, 1.992796e-1,
    };
    /** Group shares of nominal power at equilibrium; sums to {@link #EQUILIBRIUM}. */
    private static final double[] C = {
        0.00505596, 0.00239735, 0.00079283, 0.00332724, 0.00294689,
        0.00546243, 0.00461941, 0.01061061, 0.00517735, 0.02460993,
    };

    private final double[] h = new double[LAMBDA.length];

    /** Start in equilibrium at the given fission power (1.0 = reference state). */
    public DecayHeat(double fissionPower) {
        for (int i = 0; i < h.length; i++) {
            h[i] = C[i] * fissionPower;
        }
    }

    /** Advance the decay heat by {@code dt} seconds at fission power {@code p}. */
    public void step(double dt, double p) {
        for (int i = 0; i < h.length; i++) {
            double target = C[i] * p;
            double decay = Math.exp(-LAMBDA[i] * dt);
            h[i] = target + (h[i] - target) * decay;
        }
    }

    /** Total decay heat as a fraction of nominal thermal power. */
    public double total() {
        double s = 0;
        for (double hi : h) {
            s += hi;
        }
        return s;
    }

    /** Group decay constants (for the self-test and provenance). */
    public static double[] lambdas() {
        return LAMBDA.clone();
    }

    /** Group equilibrium shares (for the self-test and provenance). */
    public static double[] shares() {
        return C.clone();
    }
}
