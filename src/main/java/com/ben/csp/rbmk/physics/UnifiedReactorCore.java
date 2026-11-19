package com.ben.csp.rbmk.physics;

import com.ben.csp.rbmk.RbmkCore;
import com.ben.csp.rbmk.RbmkGeometry;

/**
 * THE core: the RBMK-1000 driven by the time-dependent two-group spatial solver.
 * The flux is the state, and the core feeds back on itself through the cross
 * sections — there is no fitted reactivity term and no accident mode.
 *
 * <p>This is the single reactor the project tests against. It supersedes the three
 * earlier cores (lumped {@link ReactorCore}, static-eigenvalue
 * {@link SpatialReactorCore}, and the open-loop {@link TransientReactorCore}) by
 * taking the one correct thing from each:
 * <ul>
 *   <li>from {@link TransientReactorCore}: the time-dependent flux is the state
 *       ({@link RbmkTransientSolver}) — no lumped reactivity amplitude;</li>
 *   <li>from {@link SpatialReactorCore}: the closed feedback loop — void, fuel
 *       temperature and xenon are computed from the power every step and fed back
 *       into the cross sections through {@link RbmkXsec#fuelCell}.</li>
 * </ul>
 *
 * <p><b>What is closed and what is not.</b> The void (through
 * {@link ChannelCooling}), the fuel temperature (Doppler) and the Xe-135 poison all
 * act through the fuel cell's cross sections, so each one changes the neutron
 * balance and the power — not merely a readout. The fuel temperature is a PER-NODE
 * field, derived from the coolant each node sits above ({@link FuelTemperature}):
 * the fuel is hot where the power is, and it tracks the coolant flow and inlet
 * temperature, so a pump trip cools the fuel with the water. The graphite
 * temperature is computed each step and fed back through {@link RbmkGraphiteTemp}
 * when the derived lattice table is present (see its gate).
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
    /**
     * Numerical step actually used by {@link #step}. Defaults to {@link #DT} and is
     * changed only by a caller that needs to run a multi-hour phase in a test: the
     * integration is backward-Euler on the stiff removal plus an exact exponential
     * on the xenon chain, so it is stable and accurate far coarser than 5 ms
     * (measured: 5 ms and 250 ms agree to ~0.1 % over 1 s of a nominal core).
     */
    private double physicsDt = DT;
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
    /** Axial node height for the fuel linear power (core height / axial nodes). */
    private static final double NODE_LENGTH_M = RbmkGeometry.CORE_HEIGHT_M / RbmkCore.AXIAL_NODES;
    /**
     * @deprecated 2026-11-15: no longer used. Xenon now enters the neutron balance
     *     through the fuel cell's thermal absorption ({@link RbmkXsec#XENON_MACRO_ABS}),
     *     so this readout-only coefficient is gone. Kept only to record the old form.
     */
    @Deprecated
    private static final double XENON_COEFF = 0.024;
    /**
     * @deprecated 2026-11-15: no longer used. This lumped point-kinetics power
     *     coefficient double-counted the temperature feedback the spatial core
     *     already carries in its cross sections, so {@link #reactivity()} dropped it.
     *     Kept only to record where the old readout came from.
     */
    @Deprecated
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
    /**
     * The 211 individual rods. The single {@code insertion} scalar above is retained
     * as the mean position the solvers drive from, but the margin needs the
     * per-rod state, so the bank is stepped alongside it.
     */
    private final RodBank rods = new RodBank();

    /** Period measurement: the window and its anchor point. */
    private static final double PERIOD_WINDOW_S = 5.0;
    private static final double PERIOD_MIN_SPAN_S = 1.0;
    private double periodRefPower;
    private double periodRefSeconds;
    private double voidFraction = NOMINAL_VOID;
    private double fuelTempC = NOMINAL_FUEL_C;
    /** Per-axial-node fuel temperature, degC, bottom to top (the field Doppler sees). */
    private double[] fuelTempByNode = new double[RbmkCore.AXIAL_NODES];
    /** The fuel-to-coolant conduction model, anchored to the sourced peak temperature. */
    private FuelTemperature fuelConduction;
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
        // The fuel temperature is part of the same fixed point: it is now a per-node
        // field (hot where the power is), and it feeds Doppler back into the shape.
        cooling.solve(1.0, NOMINAL_FLOW_KGS);
        if (axialCoupling) {
            double[] prof = selfConsistentSeed();
            voidFraction = mean(prof);
            solver.seedCriticalAxial(prof, fuelTempByNode);
            fuelTempC = mean(fuelTempByNode);
        } else {
            voidFraction = cooling.meanVoid();
            fuelConduction = anchorFuelModel();
            fuelTempC = fuelConduction.equilibrium(cooling.coolantTempC(),
                FuelTemperature.linearPowerPerRod(cooling.nodePowerW(),
                    NODE_LENGTH_M, RbmkGeometry.FUEL_CHANNELS))[0];
            java.util.Arrays.fill(fuelTempByNode, fuelTempC);
            solver.seedCritical(voidFraction, fuelTempC);
        }
        kRef = solver.balanceKeff();
        keff = kRef;
        fission = solver.power();
    }

    /**
     * Iterate (void profile from the channel energy balance) against (flux shape that
     * void produces) to the fixed point at nominal power and full flow, and along the
     * way fix the fuel-temperature field and its conduction anchor.
     *
     * <p>Two phases: first the void is converged with a uniform fuel temperature (the
     * shape has to settle before the peak power is known); then the fuel model is
     * anchored to the sourced maximum fuel temperature at that peak, and void and fuel
     * are iterated together to the joint fixed point. Converges in a handful of passes;
     * each pass is one static solve. Returns the converged per-node void profile and
     * leaves {@link #fuelTempByNode} and {@link #fuelConduction} set.
     */
    private double[] selfConsistentSeed() {
        RbmkSolver3D stat = new RbmkSolver3D();
        stat.setOmega(RbmkSolver3D.SAFE_OMEGA);
        stat.setInnerSweeps(3);
        double[] prof = cooling.voidFraction().clone();
        double[] fuel = new double[RbmkCore.AXIAL_NODES];
        java.util.Arrays.fill(fuel, NOMINAL_FUEL_C);

        // Phase 1: void fixed point with uniform fuel.
        for (int pass = 0; pass < 3; pass++) {
            prof = staticPass(stat, prof, fuel);
            if (prof == null) {
                break;   // diverged; keep the last good profile
            }
        }
        // Anchor the conduction rise to the sourced peak fuel temperature.
        fuelConduction = anchorFuelModel();

        // Phase 2: void and per-node fuel together to the joint fixed point.
        for (int pass = 0; pass < 4; pass++) {
            fuel = fuelConduction.equilibrium(cooling.coolantTempC(),
                FuelTemperature.linearPowerPerRod(cooling.nodePowerW(),
                    NODE_LENGTH_M, RbmkGeometry.FUEL_CHANNELS));
            double[] next = staticPass(stat, prof, fuel);
            if (next == null) {
                break;
            }
            prof = next;
        }
        fuelTempByNode = fuel.clone();
        seedRodBank();
        return prof;
    }

    /**
     * Put the per-rod bank where the plant actually sits at power: all rods driven to
     * the nominal insertion, so the margin starts at the state the scalar
     * {@code insertion} describes rather than at zero.
     */
    private void seedRodBank() {
        for (int i = 0; i < rods.size(); i++) {
            rods.setTarget(i, NOMINAL_INSERTION);
        }
        rods.step(1000.0, false);
    }

    /**
     * One static pass: build the cells from the given profiles, solve, and march the
     * channel energy balance up the resulting flux shape. Returns the new per-node
     * void profile, or null if the solve diverged (the caller keeps the last good).
     */
    private double[] staticPass(RbmkSolver3D stat, double[] voidByNode, double[] fuelByNode) {
        for (int x = -stat.extent(); x <= stat.extent(); x++) {
            for (int z = -stat.extent(); z <= stat.extent(); z++) {
                for (int a = 0; a < RbmkCore.AXIAL_NODES; a++) {
                    if (stat.isActive(x, z, a) && !stat.isControl(x, z, a)) {
                        stat.setCell(x, z, a, RbmkXsec.fuelCell(voidByNode[a], fuelByNode[a]));
                    }
                }
            }
        }
        stat.setRodInsertion(NOMINAL_INSERTION);
        stat.solve(400, 1e-9);
        if (stat.diverged()) {
            return null;
        }
        cooling.setShape(stat.axialShape());
        cooling.solve(1.0, NOMINAL_FLOW_KGS);
        return cooling.voidFraction().clone();
    }

    /**
     * The fuel-to-coolant conduction rise, fixed so the HOTTEST node reaches the
     * documented maximum fuel temperature at the current shape. R_eff is smaller than
     * the physical UO2+gap+clad+film chain because the model's axial peaking factor is
     * higher than a real core's; anchoring to the sourced temperature keeps the peak
     * right and is revisited when the axial shape is corrected. See
     * {@link FuelTemperature}.
     */
    private FuelTemperature anchorFuelModel() {
        double[] tc = cooling.coolantTempC();
        double[] q = FuelTemperature.linearPowerPerRod(cooling.nodePowerW(),
            NODE_LENGTH_M, RbmkGeometry.FUEL_CHANNELS);
        int hot = 0;
        for (int a = 1; a < q.length; a++) {
            if (q[a] > q[hot]) {
                hot = a;
            }
        }
        return FuelTemperature.anchoredToPeak(q[hot], tc[hot]);
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
        int steps = (int) Math.max(1, Math.round(dt / physicsDt));
        for (int i = 0; i < steps; i++) {
            physicsStep(physicsDt);
        }
    }

    /**
     * Set the numerical step used by {@link #step}, seconds. For long test phases
     * only; production runs at {@link #DT}. Values above ~0.25 s are not advised.
     */
    public void setPhysicsDt(double seconds) {
        this.physicsDt = Math.max(1e-4, seconds);
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
        // coupling is on, as a core average otherwise. The xenon ratio is carried on
        // the same path, so the poison acts through the neutron balance.
        solver.setXenonRatio(xenonRatio());
        if (axialCoupling) {
            solver.setAxialState(cooling.voidFraction(), fuelTempByNode, graphiteTempC);
        } else {
            solver.setCoreState(voidFraction, fuelTempC, graphiteTempC);
        }
        solver.setRodInsertion(insertion);
        solver.step(dt, SWEEPS);

        fission = solver.power();
        keff = solver.balanceKeff();
        simSeconds += dt;
        anchorPeriod();

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

        // Fuel: each node sits a conduction rise above ITS OWN coolant, which carries
        // the real flow and inlet temperature. The rise is the anchored fuel model, so
        // a pump trip cools the fuel with the water and a power tilt heats it where the
        // flux is. Each node relaxes toward equilibrium on the fuel time constant.
        double[] fuelEq = fuelConduction.equilibrium(cooling.coolantTempC(),
            FuelTemperature.linearPowerPerRod(cooling.nodePowerW(),
                NODE_LENGTH_M, RbmkGeometry.FUEL_CHANNELS));
        double relax = Math.min(1.0, dt / TAU_FUEL);
        double sum = 0;
        for (int a = 0; a < fuelTempByNode.length; a++) {
            fuelTempByNode[a] += (fuelEq[a] - fuelTempByNode[a]) * relax;
            sum += fuelTempByNode[a];
        }
        fuelTempC = sum / fuelTempByNode.length;
        graphiteTempC += (fuelTempC * 0.95 - graphiteTempC) * (dt / TAU_GRAPHITE);

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
        // The spatial eigenvalue against the reference critical state. ALL the
        // feedbacks are inside keff now — void and Doppler enter through the fuel
        // cell, and xenon enters through the same cell's thermal absorption — so
        // nothing is added here. Adding a term would double-count feedback the
        // solver already carries (that is why the old POWER_COEFF and XENON_COEFF
        // readout terms were removed). This is the true reactivity of the core.
        return (keff - kRef) / kRef;
    }

    @Override
    public double insertion() {
        return insertion;
    }

    /**
     * The operating reactivity margin, in equivalent fully inserted manual rods —
     * the quantity SKALA reports for request code {@code K0S0000}.
     *
     * <p>Computed from the individual rod positions in {@link RodBank}, not from the
     * single insertion scalar, because ORM is by definition the sum of the
     * individual rods' worths. Zero if the cartogram failed to load, so a broken
     * resource reads as "no margin" rather than as a plausible number.
     */
    public double orm() {
        return rods == null ? 0.0 : rods.orm();
    }

    /** The per-rod state behind {@link #orm()}. */
    public RodBank rodBank() {
        return rods;
    }

    /**
     * The reactor period, s — the time for the power to change by a factor of e,
     * measured from the core's own power history. Positive while rising, negative
     * while falling, {@link Double#POSITIVE_INFINITY} when the power is steady.
     *
     * <p>This is a <em>measurement</em>, not the prompt-period formula
     * {@code T = l/rho}: the operator's period gauge reads the log-rate of the power
     * it can see, delayed neutrons included. The window is a few seconds so the
     * reading is not dominated by a single step.
     */
    public double reactorPeriod() {
        double span = simSeconds - periodRefSeconds;
        if (span < PERIOD_MIN_SPAN_S || !(fission > 0) || !(periodRefPower > 0)) {
            return Double.NaN;
        }
        double logRatio = Math.log(fission / periodRefPower);
        if (Math.abs(logRatio) < 1e-9) {
            return Double.POSITIVE_INFINITY;
        }
        return span / logRatio;
    }

    /** Re-anchor the period measurement window; called on each physics step. */
    private void anchorPeriod() {
        if (simSeconds - periodRefSeconds >= PERIOD_WINDOW_S) {
            periodRefPower = fission;
            periodRefSeconds = simSeconds;
        }
    }

    @Override
    public double voidFraction() {
        return voidFraction;
    }

    @Override
    public double fuelTempC() {
        return fuelTempC;
    }

    /** Per-axial-node fuel temperature, degC, bottom to top (hot where the flux is). */
    public double[] fuelTempByNodeC() {
        return fuelTempByNode.clone();
    }

    /** The anchored fuel-to-coolant conduction rise, K per (W/m). */
    public double fuelRisePerWPerM() {
        return fuelConduction.risePerWPerM();
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
        // Drive the per-rod bank toward the same target. It carries its own per-type
        // travel times (АЗ 2.5 s on a scram, the rest 12 s), so a scram staggers.
        boolean scram = insertionTarget > insertion + 1e-9 || insertionTarget >= 1.0;
        for (int i = 0; i < rods.size(); i++) {
            rods.setTarget(i, insertionTarget);
        }
        rods.step(dt, scram);
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }
}
