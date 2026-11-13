package com.ben.csp.rbmk.physics;

/**
 * Two-group homogenised neutron cross sections for one node, in cm^-1, plus the
 * material library the RBMK core is built from.
 *
 * <p>Group 1 is fast, group 2 thermal. {@code sr1} is the removal cross section
 * (absorption plus down-scatter out of the fast group); {@code ss12} is the
 * down-scatter into the thermal group; {@code nsf} is nu*Sigma_f.
 *
 * <p>The values are representative of an RBMK lattice cell. They are reference
 * values to VERIFY against a NIKIET/SKALA dataset before the physics is frozen;
 * the method built on them (eigenvalue, flux shape, coefficients, rod worth) is
 * what is being validated here.
 */
public record RbmkXsec(
    double d1, double sr1, double nsf1,
    double d2, double sa2, double nsf2,
    double ss12, double ss21) {

    /** Legacy 7-argument form: no up-scatter (fallback constants only). */
    public RbmkXsec(double d1, double sr1, double nsf1,
                    double d2, double sa2, double nsf2, double ss12) {
        this(d1, sr1, nsf1, d2, sa2, nsf2, ss12, 0.0);
    }

    public RbmkXsec plus(RbmkXsec o) {
        return new RbmkXsec(d1 + o.d1, sr1 + o.sr1, nsf1 + o.nsf1,
            d2 + o.d2, sa2 + o.sa2, nsf2 + o.nsf2, ss12 + o.ss12, ss21 + o.ss21);
    }

    public RbmkXsec scale(double f) {
        return new RbmkXsec(d1 * f, sr1 * f, nsf1 * f, d2 * f, sa2 * f, nsf2 * f,
            ss12 * f, ss21 * f);
    }

    public static RbmkXsec mix(double wa, RbmkXsec a, double wb, RbmkXsec b) {
        return a.scale(wa).plus(b.scale(wb));
    }

    // ----------------------------------------------------------- materials
    public static final RbmkXsec GRAPHITE =
        new RbmkXsec(0.95, 0.0025, 0.0, 0.90, 0.0005, 0.0, 0.0023);
    public static final RbmkXsec WATER =
        new RbmkXsec(1.40, 0.045, 0.0, 0.16, 0.0200, 0.0, 0.044);
    public static final RbmkXsec STEEL =
        new RbmkXsec(0.50, 0.010, 0.0, 0.40, 0.0200, 0.0, 0.005);
    public static final RbmkXsec CONCRETE =
        new RbmkXsec(0.70, 0.008, 0.0, 0.60, 0.0150, 0.0, 0.006);
    public static final RbmkXsec B4C =
        new RbmkXsec(0.80, 0.050, 0.0, 0.45, 0.4500, 0.0, 0.010);

    // -------------------------------------------------------- RBMK cells
    /**
     * Feedback calibration. The homogenised cell's raw feedbacks are far stronger
     * than a real RBMK's, because a one-node cell concentrates the whole effect.
     * These factors scale the emergent dk/k down to the reference coefficients in
     * {@link ReactivityModel} (void +0.030 per unit, Doppler -1.0e-5/K) — the same
     * values the lumped core is built on — so the spatial and lumped cores agree
     * on the feedback physics. Verified by {@code SpatialReactorCore} self-test.
     */
    private static final double VOID_FEEDBACK_SCALE = 0.41;
    private static final double DOPPLER_FEEDBACK_SCALE = 0.020;
    // Both scales above are PLACEHOLDERS used only by legacyFuelCell below, i.e.
    // only when the derived lattice table is absent. See PhysicsProvenance
    // (xsec.void_feedback_scale, xsec.doppler_feedback_scale, marked fallback).

    /**
     * A fuel channel cell (ТК): homogenised fuel + graphite + water.
     *
     * <p>The constants come from the lattice calculation in {@code tools/lattice}
     * (evaluated ENDF/B-VIII.0 data + the real channel geometry), loaded by
     * {@link LatticeTable}. The void dependence — including the positive void
     * coefficient — is a property of that physics, not a fitted factor. The old
     * hand-set cross sections and feedback scales are kept only as a fallback for
     * the case where the resource is missing.
     */
    public static RbmkXsec fuelCell(double voidFraction, double fuelTempC) {
        RbmkXsec derived = LatticeTable.cell(voidFraction);
        if (derived != null) {
            // Doppler: resonance absorption in U-238 grows with fuel temperature,
            // which ADDS to the fast removal (a NEGATIVE reactivity feedback). The
            // scale is the U-238 resonance width; the lattice self-shielding fixes
            // the rest.
            double doppler = DOPPLER_REMOVAL_PER_K * (fuelTempC - 300.0) * derived.sr1();
            return new RbmkXsec(derived.d1(), derived.sr1() + doppler, derived.nsf1(),
                derived.d2(), derived.sa2(), derived.nsf2(), derived.ss12(), derived.ss21());
        }
        return legacyFuelCell(voidFraction, fuelTempC);
    }

    /** Fractional increase in fast removal per kelvin (U-238 Doppler). */
    private static final double DOPPLER_REMOVAL_PER_K = 1.0e-5;

    private static RbmkXsec legacyFuelCell(double voidFraction, double fuelTempC) {
        double waterFrac = 0.12 * (1.0 - voidFraction) * VOID_FEEDBACK_SCALE;
        double steamFrac = 0.12 * voidFraction * VOID_FEEDBACK_SCALE;
        RbmkXsec cell = new RbmkXsec(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
            .plus(GRAPHITE.scale(0.75))
            .plus(WATER.scale(waterFrac))
            .plus(STEAM.scale(steamFrac))
            .plus(FUEL.scale(0.13));
        double doppler = 0.010 * DOPPLER_FEEDBACK_SCALE * (fuelTempC - 300.0) / 1000.0;
        return new RbmkXsec(cell.d1, cell.sr1 + Math.max(0, doppler), cell.nsf1,
            cell.d2, cell.sa2, cell.nsf2, cell.ss12);
    }

    /** A control channel cell holding the given rod material, plus graphite. */
    public static RbmkXsec controlCell(RbmkXsec rodMaterial) {
        return controlGraphite().scale(0.75).plus(rodMaterial.scale(0.25));
    }

    /** Graphite in the control channel: derived if available, else hand-set. */
    public static RbmkXsec controlGraphite() {
        RbmkXsec d = LatticeTable.control("graphite");
        return d != null ? d : GRAPHITE;
    }

    /** B4C rod material: derived if available, else hand-set. */
    public static RbmkXsec controlB4C() {
        RbmkXsec d = LatticeTable.control("b4c");
        return d != null ? d : B4C;
    }

    /** Fuel material (UO2 + clad, homogenised contribution). */
    public static final RbmkXsec FUEL =
        new RbmkXsec(1.00, 0.028, 0.0080, 0.90, 0.090, 0.130, 0.022);

    /** Steam: almost no moderation and no absorption — the void. */
    public static final RbmkXsec STEAM =
        new RbmkXsec(0.60, 0.001, 0.0, 0.50, 0.0005, 0.0, 0.001);
}
