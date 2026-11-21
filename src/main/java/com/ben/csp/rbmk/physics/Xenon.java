package com.ben.csp.rbmk.physics;

/**
 * Xenon-135 / iodine-135 dynamics: the calculation behind the "xenon pit" that
 * poisoned the reactor after the power reduction, and a genuine part of the
 * accident. It is solved here from the same two coupled equations a Soviet
 * operator worked from, so the classic cases can be reproduced.
 *
 * <p>   dI/dt = gamma_I * Sigma_f * phi  - lambda_I * I
 * <p>   dX/dt = gamma_X * Sigma_f * phi + lambda_I * I - lambda_X * X - sigma_X * X * phi
 *
 * <p>Concentrations are in arbitrary (relative) units; the worth returned is
 * normalised to a reference so its magnitude is a few percent of reactivity, as
 * observed. Constants are the standard thermal values (mark/verify against a
 * NIKIET/SKALA source before the physics is frozen).
 */
public final class Xenon {

    public static final double LAMBDA_I = 2.926e-5;   // I-135, s^-1 (T1/2 6.57 h)
    public static final double LAMBDA_XE = 2.093e-5;  // Xe-135, s^-1 (T1/2 9.2 h)
    public static final double GAMMA_I = 0.0639;      // fission yield of I-135
    public static final double GAMMA_XE = 0.00237;    // direct fission yield of Xe-135
    public static final double SIGMA_XE = 2.65e-18;   // Xe-135 micro absorption, cm^2

    /**
     * Full-power core-average thermal flux, n/cm^2.s.
     *
     * <p>DERIVED (provenance {@code xenon.flux_full}), not fitted: phi = P / (E_f * Sigma_f
     * * V) with P = 3200 MWt, E_f = 200 MeV, Sigma_f = 0.00286 /cm (the shipped lattice
     * table) and V = 756 m3 (the homogenised core). The same inputs give 4.23 kW/litre,
     * inside the RBMK's 4-5 kW/litre band, which is the consistency check on the
     * normalisation.
     *
     * <p>This number sets how fast xenon is BURNED, so it sets the depth of the xenon
     * pit: at the old unsourced 3.0e13 the pit peaked 1.91x equilibrium, at this value
     * 2.41x. The pit is what forced the operators to withdraw rods past the ORM limit,
     * so this is on the accident path. It is still {@code to_verify} because 200 MeV and
     * nu = 2.43 are textbook rather than NIKIET values.
     */
    public static final double FLUX_FULL = 4.62e13;

    private double iodine;
    private double xenon;
    private double flux = 1.0;   // relative flux (1.0 = full power)

    /** Start in equilibrium at the given relative flux. */
    public Xenon(double relativeFlux) {
        this.flux = relativeFlux;
        this.iodine = GAMMA_I * phi() / LAMBDA_I;
        this.xenon = (GAMMA_I + GAMMA_XE) * phi() / (LAMBDA_XE + SIGMA_XE * phi());
    }

    private double phi() {
        return flux * FLUX_FULL;
    }

    public void setFlux(double relativeFlux) {
        this.flux = relativeFlux;
    }

    public double flux() {
        return flux;
    }

    public double iodine() {
        return iodine;
    }

    public double xenon() {
        return xenon;
    }

    public void step(double dt) {
        double phi = phi();
        double prodI = GAMMA_I * phi;
        double prodX = (GAMMA_I + GAMMA_XE) * phi;
        double removalX = LAMBDA_XE + SIGMA_XE * phi;

        // Exact update of the linear chain over dt (stable at any dt).
        double eI = Math.exp(-LAMBDA_I * dt);
        double i1 = iodine * eI + (prodI / LAMBDA_I) * (1 - eI);

        double eX = Math.exp(-removalX * dt);
        double term1 = xenon * eX;
        double term2 = (prodX / removalX) * (1 - eX);
        double term3 = LAMBDA_I * (iodine - prodI / LAMBDA_I) / (removalX - LAMBDA_I) * (eI - eX);
        double x1 = term1 + term2 + term3;

        iodine = i1;
        xenon = x1;
    }

    /**
     * Xenon reactivity worth (negative), relative to the full-power equilibrium:
     * -0.03 at equilibrium, more negative in the pit. The 3 % scale is a reference
     * value to verify; use it for behaviour and trends.
     */
    public double worth() {
        double ref = (GAMMA_I + GAMMA_XE) * FLUX_FULL / (LAMBDA_XE + SIGMA_XE * FLUX_FULL);
        return -0.03 * (xenon / ref);
    }
}
