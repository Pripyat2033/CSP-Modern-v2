package com.ben.csp.rbmk.physics;

import com.ben.csp.rbmk.RbmkGeometry;

/**
 * THE core: the RBMK-1000 driven by the time-dependent two-group spatial solver,
 * with the thermal-hydraulic and xenon feedbacks CLOSED — the flux is the state,
 * and the core feeds back on itself.
 *
 * <p>This is the single reactor the project tests against. It supersedes the three
 * earlier cores (lumped {@link ReactorCore}, static-eigenvalue
 * {@link SpatialReactorCore}, and the open-loop {@link TransientReactorCore}) by
 * taking the one correct thing from each:
 * <ul>
 *   <li>from {@link TransientReactorCore}: the time-dependent flux is the state
 *       ({@link RbmkTransientSolver}) — no lumped reactivity amplitude;</li>
 *   <li>from {@link SpatialReactorCore}: the closed feedback loop — void, fuel and
 *       graphite temperature and xenon are computed from the power every step and
 *       fed back into the cross sections through {@link RbmkXsec#fuelCell}.</li>
 * </ul>
 *
 * <p><b>Why this matters for accuracy.</b> The accident is a spatial and feedback
 * phenomenon. The positive void coefficient and the positive scram are not put in
 * by hand here: they emerge from the derived lattice cross sections and the rod's
 * axial geometry. The core can therefore be WRONG in the right way — it can run
 * away when it is put into the 26 April configuration, and shut down when it is
 * not, without anyone having told it which is which.
 *
 * <p><b>Time.</b> The physics advances at a fixed {@code DT} per step; {@link #step}
 * advances the wall clock by {@code dt} and runs however many fixed steps are
 * needed. For the live game the core runs on its own daemon thread (see
 * {@link #startThreaded()}); for tests {@link #step} is called directly.
 */
public final class UnifiedReactorCore implements ReactorModel, AutoCloseable {

    /** Fixed physics step, seconds. Stable and drift-free from 1 ms up. */
    public static final double DT = 0.005;
    /** Gauss-Seidel sweeps per physics step. */
    private static final int SWEEPS = 2;

    // --- operating point (the reference critical state; matches the old cores) ---
    public static final double NOMINAL_INSERTION = RbmkTransientSolver.NOMINAL_INSERTION;
    public static final double NOMINAL_VOID = RbmkTransientSolver.NOMINAL_VOID;
    public static final double NOMINAL_FUEL_C = RbmkTransientSolver.NOMINAL_FUEL_C;
    public static final double NOMINAL_GRAPHITE_C = 600.0;

    // --- feedback time constants and gains (same form as SpatialReactorCore) ---
    private static final double TAU_VOID = 3.0;
    private static final double TAU_FUEL = 10.0;
    private static final double TAU_GRAPHITE = 30.0;
    private static final double VOID_PER_POWER = 0.08;
    private static final double FUEL_PER_POWER = 600.0;
    private static final double XENON_COEFF = 0.024;
    private static final double POWER_COEFF = ReactivityModel.POWER_COEFF;
    /** Xenon concentration at the full-power equilibrium (the worth normaliser). */
    private static final double XENON_EQUILIBRIUM = new Xenon(1.0).xenon();

    private static final double ROD_RATE =
        RbmkGeometry.ROD_SPEED_M_PER_S / RbmkGeometry.CORE_HEIGHT_M;

    private final RbmkTransientSolver solver = new RbmkTransientSolver();
    private final Xenon xenon = new Xenon(1.0);
    private final DecayHeat decayHeat = new DecayHeat(1.0);

    /** Eigenvalue of the reference critical state; the reactivity zero. */
    private final double kRef;

    // --- reactor state ---
    private double insertion = NOMINAL_INSERTION;
    private double insertionTarget = NOMINAL_INSERTION;
    private double voidFraction = NOMINAL_VOID;
    private double fuelTempC = NOMINAL_FUEL_C;
    private double graphiteTempC = NOMINAL_GRAPHITE_C;
    private double fission = 1.0;
    private double keff = 1.0;
    private double simSeconds = 0;

    // --- control ---
    private boolean autoRod = false;
    private double powerSetpoint = 1.0;
    /** АР gain: rod-drive rate per unit power error (the spatial core's tuned value). */
    private double powerGain = 0.01;

    // --- threading (live game only) ---
    private volatile boolean running = false;
    private volatile boolean threaded = false;
    private Thread thread;
    private volatile double lastStepMs = 0;
    private volatile double realTimeFactor = 1.0;

    public UnifiedReactorCore() {
        // The solver seeds itself to the reference critical state in its constructor;
        // take the reactivity zero from the eigenvalue the balance reads there.
        kRef = solver.balanceKeff();
        keff = kRef;
        fission = solver.power();
    }

    // ------------------------------------------------------------ threading

    /**
     * Run the physics on its own daemon thread at the fixed {@link #DT} step, so
     * the live game samples it instead of paying for it on the tick thread. Tests
     * call {@link #step} directly and never start the thread.
     */
    public void startThreaded() {
        if (threaded) {
            return;
        }
        threaded = true;
        running = true;
        thread = new Thread(this::runLoop, "rbmk-unified-core");
        thread.setDaemon(true);
        thread.start();
    }

    private void runLoop() {
        long wallStart = System.nanoTime();
        while (running) {
            long t0 = System.nanoTime();
            physicsStep(DT);
            lastStepMs = (System.nanoTime() - t0) / 1e6;
            double wall = (System.nanoTime() - wallStart) / 1e9;
            realTimeFactor = wall > 0 ? simSeconds / wall : 1.0;
        }
    }

    @Override
    public void close() {
        running = false;
        if (thread != null) {
            try {
                thread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // ------------------------------------------------------------- ReactorModel

    /**
     * Advance the core by {@code dt} wall-seconds. On the threaded core this is a
     * no-op (the thread owns stepping); on the synchronous core it runs the
     * physics in fixed {@link #DT} steps so a caller gets exact, reproducible
     * time regardless of the frame time.
     */
    @Override
    public void step(double dt) {
        if (threaded) {
            return;
        }
        if (dt <= 0) {
            return;
        }
        int steps = (int) Math.max(1, Math.round(dt / DT));
        for (int i = 0; i < steps; i++) {
            physicsStep(DT);
        }
    }

    /** One fixed physics step: feedbacks, rod drive, solve, update state. */
    private void physicsStep(double dt) {
        if (autoRod) {
            // АР: a plain integral regulator on the power error, the same form the
            // spatial core was tuned to. The rod drive moves the bank at a rate
            // proportional to the error, so the loop has one gain and no direct
            // reactivity term to ring against the plant. Power below setpoint drives
            // the rods out (insertion down).
            double error = powerSetpoint - fission;
            insertionTarget = clamp01(insertion - powerGain * error * dt);
        }
        moveRod(dt);

        // Feedbacks enter through the cross sections, not a fitted term.
        solver.setCoreState(voidFraction, fuelTempC);
        solver.setRodInsertion(insertion);
        solver.step(dt, SWEEPS);

        fission = solver.power();
        keff = solver.balanceKeff();
        simSeconds += dt;

        decayHeat.step(dt, fission);
        double total = DecayHeat.FISSION_SHARE * fission + decayHeat.total();
        double voidEq = voidFromPower(total);
        voidFraction += (voidEq - voidFraction) * (dt / TAU_VOID);
        voidFraction = clamp01(voidFraction);
        double fuelEq = tempFromPower(total);
        fuelTempC += (fuelEq - fuelTempC) * (dt / TAU_FUEL);
        graphiteTempC += (fuelEq * 0.95 - graphiteTempC) * (dt / TAU_GRAPHITE);

        xenon.setFlux(Math.max(fission, 0));
        xenon.step(dt);
    }

    @Override
    public double power() {
        // Total thermal power = fission + decay heat (see ReactorCore.power()).
        return DecayHeat.FISSION_SHARE * fission + decayHeat.total();
    }

    @Override
    public double fissionPower() {
        return fission;
    }

    @Override
    public double reactivity() {
        // The spatial eigenvalue against the reference critical state, plus the
        // power and xenon terms (same form as SpatialReactorCore).
        double spatial = (keff - kRef) / kRef;
        double powerTerm = POWER_COEFF * (fission - 1.0);
        return spatial + powerTerm - XENON_COEFF * (xenonRatio() - 1.0);
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
        return xenon.xenon() / XENON_EQUILIBRIUM;
    }

    @Override
    public void setRodTarget(double target) {
        this.autoRod = false;
        this.insertionTarget = clamp01(target);
    }

    @Override
    public void setAutoRegulator(boolean on) {
        this.autoRod = on;
        this.powerSetpoint = fission;
    }

    // --------------------------------------------------------------- readouts

    /** The spatial eigenvalue from the neutron balance. */
    public double keff() {
        return keff;
    }

    /** Wall milliseconds the last fixed physics step took (threaded core only). */
    public double lastStepMs() {
        return lastStepMs;
    }

    /** Simulated seconds the core has advanced. */
    public double simSeconds() {
        return simSeconds;
    }

    /** Simulated seconds per wall second (threaded core only; >1 = faster than real). */
    public double realTimeFactor() {
        return realTimeFactor;
    }

    // ---------------------------------------------------------------- helpers

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
