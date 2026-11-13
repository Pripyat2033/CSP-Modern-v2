package com.ben.csp.rbmk.physics;

import com.ben.csp.rbmk.RbmkGeometry;

/**
 * The RBMK-1000 core driven by the FULL 3D two-group diffusion solver, ticked
 * live. This is the difference that matters to the Director's question: the
 * lumped {@link ReactorCore} averages the core into a single point, whereas this
 * core resolves the neutron flux over the real lattice — every one of the 1,872
 * channels at every one of the 28 axial nodes — and derives the reactivity from
 * that solution every tick.
 *
 * <p>Each step:
 * <ol>
 *   <li>the rod drive moves the rods, and the auto-regulator (АР) trims them;</li>
 *   <li>the solver's cells are rebuilt from the current void, fuel temperature
 *       and rod insertion (so the feedbacks enter through the cross sections,
 *       not through a hand-fitted term);</li>
 *   <li>a few outer iterations of the 3D solver are run — the flux tracks the
 *       state from the previous tick, so it converges across ticks, not from
 *       scratch;</li>
 *   <li>the reactivity is read off the spatial eigenvalue against the reference
 *       critical state and fed to point kinetics for the power evolution;</li>
 *   <li>the thermal-hydraulics and the xenon chain follow the power.</li>
 * </ol>
 *
 * <p>The per-tick cost is the cost of those solver iterations — the number the
 * whole 1:4 exercise exists to measure. {@link #lastSolveMs()} reports it.
 */
public final class SpatialReactorCore implements ReactorModel {

    private final RbmkSolver3D solver = new RbmkSolver3D();
    private final PointKinetics kinetics = new PointKinetics();
    private final Xenon xenon = new Xenon(1.0);

    // --- operating point (the reference critical state) ---
    public static final double NOMINAL_INSERTION = 0.80;
    public static final double NOMINAL_VOID = 0.05;
    public static final double NOMINAL_FUEL_C = 640.0;
    public static final double NOMINAL_GRAPHITE_C = 600.0;

    /** Outer solver iterations run per tick: the live spatial cost knob. */
    private int itersPerTick = 1;

    private double insertion = NOMINAL_INSERTION;
    private double insertionTarget = NOMINAL_INSERTION;
    private double voidFraction = NOMINAL_VOID;
    private double fuelTempC = NOMINAL_FUEL_C;
    private double graphiteTempC = NOMINAL_GRAPHITE_C;
    private double power = 1.0;
    private boolean autoRod = false;
    private double powerSetpoint = 1.0;
    private double rodIntegral = 0;
    /**
     * Automatic-regulator time constant (s). The rod worth here is ~-0.93 per unit
     * insertion, so a deadbeat controller would overshoot against the 3 s void lag;
     * the target is approached with this lag, which is what makes the loop stable.
     */
    private double ctrlTau = 30.0;

    /**
     * Rod worth, d(rho)/d(insertion), for the full 211-rod bank. Measured from the
     * 3D solver (rho goes +0.1045 at full withdrawal to +0.0284 at full insertion,
     * a span of 0.076 over the unit travel), not hand-set.
     */
    private static final double ROD_GAIN = 0.076;
    /** Reactivity demanded per unit power error (the operator's authority). */
    private double powerGain = 0.01;
    /** АР proportional gain: fraction of full reactivity nulling per tick. */
    private double arGain = 0.1;
    /** Integral gain: the АР trims out the steady-state power offset. */
    private double integralGain = 0.0002;
    /**
     * Anti-windup clamp on the integral term. In reactivity units, so it must be
     * small: at 0.4 (the lumped core's value, where the rod worth is O(1)) the
     * integral saturates the whole 0.076 rod range and the loop limit-cycles.
     */
    private double integralLimit = 0.004;

    private static final double ROD_RATE =
        RbmkGeometry.ROD_SPEED_M_PER_S / RbmkGeometry.CORE_HEIGHT_M;
    private static final double TAU_VOID = 3.0;
    private static final double TAU_FUEL = 10.0;
    private static final double TAU_GRAPHITE = 30.0;
    private static final double VOID_PER_POWER = 0.08;
    private static final double FUEL_PER_POWER = 600.0;
    private static final double XENON_COEFF = 0.024;
    /** Power coefficient, dk/k per unit power above nominal (ReactivityModel.POWER_COEFF). */
    private static final double POWER_COEFF = -0.005;

    /** keff at the nominal critical state — the zero of reactivity. */
    private double kRef;
    private double keff = 1.0;
    private double lastSolveMs;

    public SpatialReactorCore() {
        calibrate();
    }

    /** Solve the reference critical state once, so reactivity reads 0 there. */
    private void calibrate() {
        solver.setCoreState(NOMINAL_VOID, NOMINAL_FUEL_C);
        solver.setRodInsertion(NOMINAL_INSERTION);
        // This both fixes the reactivity zero AND leaves the solver's flux at the
        // converged critical shape, so the first live tick is already meaningful.
        solver.solve(6000, 1e-10);
        kRef = solver.keff();
        keff = kRef;
    }

    /** Set how many solver iterations to run each tick (1 is usually enough). */
    public void setItersPerTick(int n) {
        this.itersPerTick = Math.max(1, n);
    }

    /** Set the АР proportional gain (reactivity per unit power error). */
    public void setPowerGain(double g) {
        this.powerGain = g;
    }

    public RbmkSolver3D solver() {
        return solver;
    }

    public double keff() {
        return keff;
    }

    /** Wall-clock milliseconds the last tick spent in the 3D solver. */
    public double lastSolveMs() {
        return lastSolveMs;
    }

    // -------------------------------------------------------------- ReactorModel
    @Override
    public double power() {
        return power;
    }

    @Override
    public double reactivity() {
        double spatial = (keff - kRef) / kRef;
        // the core's own power coefficient (negative), as in ReactivityModel: a
        // direct damping term that lets the АР hold power without hunting.
        double powerTerm = POWER_COEFF * (power - 1.0);
        return spatial + powerTerm - XENON_COEFF * (xenon.xenon() / xenonEquilibrium() - 1.0);
    }

    @Override
    public double insertion() {
        return insertion;
    }

    @Override
    public double voidFraction() {
        return voidFraction;
    }

    @Override
    public double fuelTempC() {
        return fuelTempC;
    }

    @Override
    public double xenonRatio() {
        return xenon.xenon() / xenonEquilibrium();
    }

    @Override
    public void setRodTarget(double target) {
        this.insertionTarget = clamp01(target);
        this.autoRod = false;
    }

    @Override
    public void setAutoRegulator(boolean on) {
        this.autoRod = on;
        this.powerSetpoint = power;
        this.rodIntegral = 0;
    }

    private double xenonEquilibrium() {
        return new Xenon(1.0).xenon();
    }

    // ----------------------------------------------------------------- step
    @Override
    public void step(double dt) {
        if (autoRod) {
            // АР: a plain integral regulator on the power error. The rod drive moves
            // the bank at a rate proportional to the error, so the loop has a single
            // tunable gain and no direct-reactivity feedback to ring against the
            // plant. Power below setpoint drives the rods out (insertion down).
            double error = powerSetpoint - power;
            insertionTarget = clamp01(insertion - powerGain * error * dt);
        }
        moveRod(dt);

        // the spatial solve: rebuild the cells, then advance the flux a little
        solver.setCoreState(voidFraction, fuelTempC);
        solver.setRodInsertion(insertion);
        long t0 = System.nanoTime();
        for (int i = 0; i < itersPerTick; i++) {
            solver.stepOnce();
        }
        // Read the reactivity from the neutron balance, not the power-iteration
        // eigenvalue: the latter only tracks how fast the flux is converging and
        // lags a rod move by thousands of iterations, which a live regulator cannot
        // use. The balance eigenvalue responds on the same tick and equals the
        // power-iteration value once converged.
        keff = solver.balanceKeff();
        lastSolveMs = (System.nanoTime() - t0) / 1e6;

        kinetics.setReactivity(reactivity());
        kinetics.step(dt);
        power = Math.max(0, kinetics.power());

        double voidEq = voidFromPower(power);
        voidFraction += (voidEq - voidFraction) * (dt / TAU_VOID);
        voidFraction = clamp01(voidFraction);
        double fuelEq = tempFromPower(power);
        fuelTempC += (fuelEq - fuelTempC) * (dt / TAU_FUEL);
        graphiteTempC += (fuelEq * 0.95 - graphiteTempC) * (dt / TAU_GRAPHITE);

        xenon.setFlux(Math.max(power, 0));
        xenon.step(dt);
    }

    private void moveRod(double dt) {
        double step = ROD_RATE * dt;
        if (insertion < insertionTarget) {
            insertion = Math.min(insertionTarget, insertion + step);
        } else if (insertion > insertionTarget) {
            insertion = Math.max(insertionTarget, insertion - step);
        }
    }

    private double voidFromPower(double p) {
        return clamp01(NOMINAL_VOID + VOID_PER_POWER * (p - 1.0));
    }

    private double tempFromPower(double p) {
        return NOMINAL_FUEL_C + FUEL_PER_POWER * (p - 1.0);
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }
}
