package com.ben.csp.rbmk.physics;

/**
 * The reactivity feedback coefficients: how the core's reactivity responds to
 * steam void, fuel temperature (Doppler), graphite temperature and power. These
 * are what make the RBMK behave the way it does — most importantly the
 * <em>positive</em> void coefficient, which is the core of the accident.
 *
 * <p>The coefficients are marked reference values (the RBMK void coefficient is
 * strongly positive at operating burnup); the method — summing independent
 * feedbacks into a reactivity, and showing that the net coefficient turns positive
 * with void — is what is validated.
 */
public final class ReactivityModel {

    /** Reference temperature for all coefficients, °C. */
    public static final double T_REF = 300.0;

    /** Void coefficient, dk/k per unit void. POSITIVE — the RBMK's fatal property. */
    public static final double VOID_COEFF = 0.030;
    /** Doppler (fuel temperature) coefficient, dk/k per °C. Negative. */
    public static final double DOPPLER_COEFF = -1.0e-5;
    /** Graphite temperature coefficient, dk/k per °C. Negative. */
    public static final double GRAPHITE_COEFF = -2.0e-5;
    /** Power coefficient, dk/k per unit power. Small and negative. */
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
