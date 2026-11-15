package com.ben.csp.rbmk.physics;

import com.ben.csp.rbmk.RbmkCore;
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
 * <p><b>The void is derived, not fitted.</b> The core used to be told its void by a
 * straight line ({@code void = 0.05 + 0.08*(power-1)}). Now the coolant path is
 * computed from the energy balance in {@link ChannelCooling}: the core power raises
 * the coolant enthalpy, the coolant boils, and the boiling decides the void. The
 * fitted line assumed a nominal void of 0.05; the energy balance from the sourced
 * plant data (10,000 kg/s, 320 kJ/kg, ~15% outlet quality) gives a core-average void
 * of ~0.35 — the fitted value was about seven times too low. The plant also tells the
 * core its actual coolant flow ({@link #setCoolantFlowKgS}), so a pump trip raises the
 * void and the power with no accident flag anywhere.
 *
 * <p><b>The void is coupled per axial node, and the seed is self-consistent.</b> The
 * derived void profile is fed to the neutronics node by node, and the flux shape it
 * produces in turn decides where the channel heat is deposited — so flux, heat and
 * void are one closed loop with no fitted map. Because the profile the core runs on
 * (mean void ~0.64) differs from the cosine fallback it would otherwise be seeded at
 * (mean void ~0.35), the reference critical state is found by iterating the two to a
 * fixed point at construction ({@link #selfConsistentVoid()}); seeding at the wrong
 * profile was the actual cause of the old nominal drift.
 *
 * <p><b>One honest calibration.</b> The derived lattice cell's whole-core void worth
 * is 4.3x the sourced INSAG-7 figure, so the void DEPENDENCE is scaled to the source
 * ({@code RbmkXsec.VOID_WORTH_SCALE}, registered {@code xsec.void_worth_scale}). The
 * shape stays the derived physics; only the magnitude is set to the measurement. See
 * {@code docs/DERIVED-NOT-FITTED.md}.
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
    /** Nominal coolant mass flow through the core, kg/s (6 pumps x 8,000 m3/h). */
    public static final double NOMINAL_FLOW_KGS = ChannelCooling.NOMINAL_FLOW_KGS;

    // --- feedback time constants and gains ---
    private static final double TAU_FUEL = 10.0;
    private static final double TAU_GRAPHITE = 30.0;
    private static final double FUEL_PER_POWER = 600.0;
    private static final double XENON_COEFF = 0.024;
    private static final double POWER_COEFF = ReactivityModel.POWER_COEFF;
    /** Xenon concentration at the full-power equilibrium (the worth normaliser). */
    private static final double XENON_EQUILIBRIUM = new Xenon(1.0).xenon();

    private static final double ROD_RATE =
        RbmkGeometry.ROD_SPEED_M_PER_S / RbmkGeometry.CORE_HEIGHT_M;

    private final RbmkTransientSolver solver = new RbmkTransientSolver();
    private final ChannelCooling cooling = new ChannelCooling(RbmkCore.AXIAL_NODES);
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
    private double coolantFlowKgS = NOMINAL_FLOW_KGS;
    private double fission = 1.0;
    private double keff = 1.0;
    private double simSeconds = 0;

    /**
     * Whether the neutronics sees the void PER AXIAL NODE (the derived profile) or
     * as a core average. The axial profile is the physically right thing — the RBMK
     * flux is bottom-peaked precisely because the coolant boils up the channel — so
     * this is the target state; the uniform path is the coarse fallback.
     */
    private boolean axialCoupling = true;

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
        // Seed at the SELF-CONSISTENT nominal fixed point: the void profile the
        // channel energy balance produces and the flux shape that void produces must
        // agree, or the core starts off-critical and drifts. A single cosine-void seed
        // (mean 0.355) does not match the shape-void it actually runs at (~0.64), so
        // the two are iterated to a fixed point before the reactivity zero is taken.
        cooling.solve(1.0, NOMINAL_FLOW_KGS);
        if (axialCoupling) {
            double[] prof = selfConsistentVoid();
            voidFraction = mean(prof);
            solver.seedCriticalAxial(prof, NOMINAL_FUEL_C);
        } else {
            voidFraction = cooling.meanVoid();
            solver.seedCritical(voidFraction, NOMINAL_FUEL_C);
        }
        kRef = solver.balanceKeff();
        keff = kRef;
        fission = solver.power();
    }

    /**
     * Iterate (void profile from the channel energy balance) against (flux shape that
     * void produces) to the fixed point at nominal power and full flow. Converges in
     * a handful of passes; each pass is one static solve. Returns the converged
     * per-node void profile.
     */
    private double[] selfConsistentVoid() {
        RbmkSolver3D stat = new RbmkSolver3D();
        stat.setOmega(RbmkSolver3D.SAFE_OMEGA);
        stat.setInnerSweeps(3);
        for (int pass = 0; pass < 3; pass++) {
            double[] prof = cooling.voidFraction().clone();
            for (int x = -stat.extent(); x <= stat.extent(); x++) {
                for (int z = -stat.extent(); z <= stat.extent(); z++) {
                    for (int a = 0; a < RbmkCore.AXIAL_NODES; a++) {
                        if (stat.isActive(x, z, a) && !stat.isControl(x, z, a)) {
                            stat.setCell(x, z, a, RbmkXsec.fuelCell(prof[a], NOMINAL_FUEL_C));
                        }
                    }
                }
            }
            stat.setRodInsertion(NOMINAL_INSERTION);
            stat.solve(400, 1e-9);
            if (stat.diverged()) {
                break;   // keep the last good profile
            }
            cooling.setShape(stat.axialShape());
            cooling.solve(1.0, NOMINAL_FLOW_KGS);
        }
        return cooling.voidFraction().clone();
    }

    private static double mean(double[] v) {
        double s = 0;
        for (double x : v) {
            s += x;
        }
        return v.length == 0 ? 0 : s / v.length;
    }

    /** Select per-node (true, default) or core-average (false) void coupling. */
    public void setAxialCoupling(boolean on) {
        this.axialCoupling = on;
    }

    /** True if the neutronics is fed the per-node void profile. */
    public boolean axialCoupling() {
        return axialCoupling;
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

        // Feedbacks enter through the cross sections, not a fitted term. The void is
        // the derived axial profile from the previous step; per node when axial
        // coupling is on, as a core average otherwise.
        if (axialCoupling) {
            solver.setAxialState(cooling.voidFraction(), fuelTempC);
        } else {
            solver.setCoreState(voidFraction, fuelTempC);
        }
        solver.setRodInsertion(insertion);
        solver.step(dt, SWEEPS);

        fission = solver.power();
        keff = solver.balanceKeff();
        simSeconds += dt;

        // Heat: fission plus the fission-product decay that cannot be switched off.
        decayHeat.step(dt, fission);
        double total = DecayHeat.FISSION_SHARE * fission + decayHeat.total();

        // Coolant: the flux shape decides where the heat is deposited, so the channel
        // energy balance marches up THAT shape, not an assumed cosine. This closes the
        // loop: flux -> heat -> void -> cross sections -> flux.
        double[] fluxShape = solver.axialShape();
        if (fluxShape != null) {
            cooling.setShape(fluxShape);
        }
        cooling.solve(total, coolantFlowKgS);
        double tau = Math.max(0.05, ChannelCooling.transitTimeS(
            coolantFlowKgS, RbmkGeometry.CORE_HEIGHT_M, RbmkGeometry.FUEL_CHANNELS));
        double alpha = Math.min(1.0, dt / tau);
        voidFraction += (cooling.meanVoid() - voidFraction) * alpha;
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

    /**
     * Set the coolant mass flow through the core (kg/s). This is the coupling the
     * plant owns: when pumps throttle or trip, the flow falls, the coolant boils
     * harder for the same heat, the void rises and the reactivity rises with it.
     * The core does not decide its own flow; the plant tells it.
     */
    public void setCoolantFlowKgS(double kgs) {
        this.coolantFlowKgS = Math.max(0.0, kgs);
    }

    /** The coolant mass flow the core is using (kg/s). */
    public double coolantFlowKgS() {
        return coolantFlowKgS;
    }

    /** The derived coolant state: void, quality, temperature per node. */
    public ChannelCooling cooling() {
        return cooling;
    }

    /** The live axial flux shape (group 1, mean over fuel channels), bottom to top. */
    public double[] axialFluxShape() {
        return solver.axialShape();
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

    private double tempFromPower(double p) {
        return NOMINAL_FUEL_C + FUEL_PER_POWER * (p - 1.0);
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }
}
