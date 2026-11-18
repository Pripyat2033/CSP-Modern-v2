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
     * {@link ReactivityModel} (void +0.020 per unit, Doppler -1.2e-5/K, per INSAG-7
     * Table II-I) — the same values the lumped core is built on — so the spatial and
     * lumped cores agree on the feedback physics. Verified by {@code SpatialReactorCore} self-test.
     */
    private static final double VOID_FEEDBACK_SCALE = 0.41;
    private static final double DOPPLER_FEEDBACK_SCALE = 0.020;
    // Both scales above are PLACEHOLDERS used only by legacyFuelCell below, i.e.
    // only when the derived lattice table is absent. See PhysicsProvenance
    // (xsec.void_feedback_scale, xsec.doppler_feedback_scale, marked fallback).

    /**
     * Scale on the DERIVED lattice void dependence, to match the sourced whole-core
     * void coefficient. The lattice calculation gives a whole-core dk_eff/dvoid of
     * +0.086 per unit void; INSAG-7 Table II-I gives +0.020 at the working point —
     * 4.3x smaller. The lattice value is the OUTLIER, and until it is explained the
     * sourced figure is the authority (SGB: a sourced number beats a computed one).
     * So the derived cell is blended toward its zero-void baseline by this factor:
     * the SHAPE of the void dependence stays the derived physics, its MAGNITUDE is
     * set to the source. Measured and registered: {@code xsec.void_worth_scale}.
     * This is a calibration, not a guess — and it is flagged {@code to_verify}
     * until the lattice void worth is reconciled.
     */
    private static final double VOID_WORTH_SCALE = 0.233;

    /**
     * The value of the hand-set void-worth dial the reactor is running. Exposed so
     * the diagnostic ({@code /rbmk dial}) can show it, and so a self-test can assert
     * that turning it to 1.0 changes the void worth — i.e. the dial is load-bearing.
     */
    public static double voidWorthScale() {
        return VOID_WORTH_SCALE;
    }

    /**
     * Xe-135 thermal absorption added to the fuel cell, cm^-1, per unit of xenon
     * ratio ABOVE the reference (equilibrium) concentration.
     *
     * <p>DERIVED, not fitted: {@code sigma_Xe * N_Xe_eq}, with the equilibrium
     * Xe-135 concentration from the iodine/xenon chain,
     * <pre>
     *   N_Xe_eq = (gamma_I + gamma_Xe) * Sigma_f * phi / (lambda_Xe + sigma_Xe * phi)
     * </pre>
     * at the full-power flux. With {@code sigma_Xe} = 2.65e-18 cm^2 ({@link Xenon#SIGMA_XE}),
     * {@code Sigma_f} ~ 2.56e-3 cm^-1 (the cell's nsf2 / nu), and phi =
     * {@link Xenon#FLUX_FULL} = 3e13: N_Xe_eq ~ 5.07e13 cm^-3, so the macro absorption
     * is ~1.34e-4 cm^-1. The measured sensitivity is dk/dsa2 ~ -160, giving a worth of
     * ~-0.021 dk/k for the full equilibrium concentration. This REPLACES the old fitted
     * readout terms (-0.024 in the lumped cores, -0.03 in {@link Xenon#worth()}): xenon
     * now acts through the neutron balance, not as a number nothing reads. The one
     * to-verify input is the reference flux (already flagged); registered as
     * {@code xenon.macro_abs} in PhysicsProvenance.
     */
    private static final double XENON_MACRO_ABS = 1.34e-4;

    /**
     * Blend a cell toward a baseline by a factor f: {@code base + f*(cell-base)}.
     * Scaling the void DEPENDENCE while holding the zero-void state exact.
     */
    private static RbmkXsec blend(RbmkXsec base, RbmkXsec cell, double f) {
        return new RbmkXsec(
            base.d1() + f * (cell.d1() - base.d1()),
            base.sr1() + f * (cell.sr1() - base.sr1()),
            base.nsf1() + f * (cell.nsf1() - base.nsf1()),
            base.d2() + f * (cell.d2() - base.d2()),
            base.sa2() + f * (cell.sa2() - base.sa2()),
            base.nsf2() + f * (cell.nsf2() - base.nsf2()),
            base.ss12() + f * (cell.ss12() - base.ss12()),
            base.ss21() + f * (cell.ss21() - base.ss21()));
    }

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
        return fuelCell(voidFraction, fuelTempC, 1.0);
    }

    /**
     * Fuel cell with the Xe-135 poison included in the thermal absorption.
     *
     * <p>{@code xenonRatio} is the Xe-135 concentration relative to its full-power
     * equilibrium (1.0 = equilibrium, as {@code /rbmk status} reports). The poison adds
     * {@code XENON_MACRO_ABS * (xenonRatio - 1.0)} to {@code sa2}, so it enters the
     * neutron BALANCE — it changes k_eff and the power, not merely a displayed number.
     * At equilibrium (ratio 1.0) the term is zero, so the reference critical state is
     * unchanged and {@code kRef} stays valid. A rising xenon ratio (the pit) removes
     * thermal neutrons and drives the core sub-critical; the operator then withdraws
     * rods to hold power, exactly as at Chernobyl.
     */
    public static RbmkXsec fuelCell(double voidFraction, double fuelTempC, double xenonRatio) {
        return fuelCell(voidFraction, fuelTempC, xenonRatio, VOID_WORTH_SCALE);
    }

    /**
     * The void-worth dial, made a parameter so the diagnostic can measure the cell
     * with the dial ON (what the reactor runs) and OFF (the raw derived physics).
     * {@code voidWorthScale == 1.0} is the undialed cell.
     */
    public static RbmkXsec fuelCell(double voidFraction, double fuelTempC, double xenonRatio,
                                    double voidWorthScale) {
        RbmkXsec derived = LatticeTable.cell(voidFraction);
        if (derived != null) {
            // Match the sourced whole-core void worth: blend the derived cell toward
            // its zero-void state. The SHAPE of the void dependence is the derived
            // physics; its magnitude is set to INSAG-7 (see VOID_WORTH_SCALE).
            RbmkXsec baseline = LatticeTable.cell(0.0);
            if (baseline != null) {
                derived = blend(baseline, derived, voidWorthScale);
            }
            // Doppler: resonance absorption in U-238 grows with fuel temperature,
            // which ADDS to the fast removal (a NEGATIVE reactivity feedback). The
            // scale is the U-238 resonance width; the lattice self-shielding fixes
            // the rest.
            double doppler = DOPPLER_REMOVAL_PER_K * (fuelTempC - 300.0) * derived.sr1();
            // Xenon: thermal absorption from the Xe-135 built up by the iodine chain.
            double xenon = XENON_MACRO_ABS * (xenonRatio - 1.0);
            return new RbmkXsec(derived.d1(), derived.sr1() + doppler, derived.nsf1(),
                derived.d2(), derived.sa2() + xenon, derived.nsf2(), derived.ss12(), derived.ss21());
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
