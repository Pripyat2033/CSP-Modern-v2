package com.ben.csp.rbmk.physics;

/**
 * The reactivity feedback coefficients: how the core's reactivity responds to
 * steam void, fuel temperature (Doppler), graphite temperature and power. These
 * are what make the RBMK behave the way it does — most importantly the
 * <em>positive</em> void coefficient, which is the core of the accident.
 *
 * <p>The coefficients are drawn from INSAG-7 Table II-I (p. 105), the main design
 * table for Chernobyl Unit 4, and are registered in {@link PhysicsProvenance}:
 * the void and Doppler coefficients are {@code verified} (sourced, with a quote);
 * the rest remain {@code to_verify} and are flagged by the build. One value is
 * knowingly unresolved: INSAG-7 gives the <em>graphite</em> temperature
 * coefficient as <em>positive</em> (+6e-5 per degC, a documented RBMK
 * destabilising property), whereas this model still carries it negative — a
 * conflict recorded, not silently changed.
 */
public final class ReactivityModel {

    /** Reference temperature for all coefficients, °C. */
    public static final double T_REF = 300.0;

    /**
     * Void coefficient, dk/k per unit void. POSITIVE — the RBMK's fatal property.
     * INSAG-7 Table II-I: +2.0e-4 per % void = 0.020 per unit void fraction.
     */
    public static final double VOID_COEFF = 0.020;
    /** Doppler (fuel temperature) coefficient, dk/k per °C. Negative.
     *  INSAG-7 Table II-I: -1.2e-5 per degC. */
    public static final double DOPPLER_COEFF = -1.2e-5;
    /**
     * Graphite temperature coefficient, dk/k per °C.
     * CONFLICT: INSAG-7 Table II-I gives +6e-5 (positive); this model still uses
     * a negative value. See PhysicsProvenance {@code feedback.graphite_coeff}.
     * TO VERIFY.
     */
    public static final double GRAPHITE_COEFF = -2.0e-5;
    /** Power coefficient, dk/k per unit power. Small and negative. TO VERIFY. */
    public static final double POWER_COEFF = -0.005;

    private ReactivityModel() {}

    /** Total reactivity from the state, in dk/k. */
    public static double reactivity(double voidFraction, double fuelTempC,
                                    double graphiteTempC, double powerFraction) {
        return VOID_COEFF * voidFraction
            + DOPPLER_COEFF * (fuelTempC - T_REF)
            + GRAPHITE_COEFF * (graphiteTempC - T_REF)
            + POWER_COEFF * powerFraction;
    }

    /**
     * The net void coefficient in the working range: the direct void term plus the
     * Doppler term the void induces (more void -> hotter fuel -> negative Doppler).
     * Returns dk/k per unit void. At high void the positive term wins, which is the
     * RBMK's dangerous regime.
     */
    public static double netVoidCoefficient(double voidFraction) {
        double dFuelPerVoid = 800.0; // °C of fuel rise per unit void (reference)
        return VOID_COEFF + DOPPLER_COEFF * dFuelPerVoid;
    }

    /** True when the net coefficient is positive at this void — the accident regime. */
    public static boolean isPositiveVoid(double voidFraction) {
        return netVoidCoefficient(voidFraction) > 0;
    }
}
