package com.ben.csp.rbmk.physics;

import com.ben.csp.rbmk.RbmkGeometry;

/**
 * The live reactor core, driven by the time-dependent spatial solver on its OWN
 * THREAD. This is how the full 3D flux runs inside Minecraft without stalling the
 * game: the physics owns a thread and a fixed 1 ms step; the server thread only
 * READS the latest published state.
 *
 * <p>Why a thread: one 1 ms step of the two-group solver over 52,416 nodes costs
 * ~2 ms on this machine. Fifty of those in a 50 ms tick would consume the whole
 * tick budget. So the solver runs continuously in the background and the world
 * samples it — the mesh never drives the physics and the physics never waits on
 * the mesh. The rod commands are volatile targets the thread picks up.
 *
 * <p>The physics is deliberately decoupled from the render: this object is a
 * {@link ReactorModel}, so the plant, the commands and any HUD read it exactly as
 * they read the lumped core.
 */
public final class TransientReactorCore implements ReactorModel, AutoCloseable {

    private static final double DT = 0.001;      // fixed 1 ms physics step
    private static final int SWEEPS = 2;         // Gauss-Seidel passes per step

    private final RbmkTransientSolver solver = new RbmkTransientSolver();
    private volatile boolean running = true;
    private Thread thread;

    // ---- commands from the server thread (volatile; the physics thread obeys) ----
    private volatile double insertionTarget = RbmkTransientSolver.NOMINAL_INSERTION;
    private volatile boolean autoRod = false;
    private volatile double powerSetpoint = 1.0;

    // ---- state published to the server thread ----
    private volatile double power = 1.0;
    private volatile double peak = 1.0;
    private volatile double insertion = RbmkTransientSolver.NOMINAL_INSERTION;
    private volatile double voidFraction = 0.05;
    private volatile double fuelTempC = 640.0;
    private volatile double xenon = 1.0;
    private volatile double simSeconds = 0;
    private volatile double stepMs = 0;
    private volatile double realTimeFactor = 1.0;

    private static final double ROD_RATE =
        RbmkGeometry.ROD_SPEED_M_PER_S / RbmkGeometry.CORE_HEIGHT_M;

    public TransientReactorCore() {
        thread = new Thread(this::run, "rbmk-transient-core");
        thread.setDaemon(true);
        thread.start();
    }

    private void run() {
        long wallStart = System.nanoTime();
        while (running) {
            double ins = insertion;
            if (autoRod) {
                // gentle proportional trim toward the power setpoint
                double err = powerSetpoint - power;
                insertionTarget = clamp01(insertionTarget - 0.05 * err * DT);
            }
            double step = ROD_RATE * DT;
            if (ins < insertionTarget) {
                ins = Math.min(insertionTarget, ins + step);
            } else if (ins > insertionTarget) {
                ins = Math.max(insertionTarget, ins - step);
            }
            solver.setRodInsertion(ins);

            long t0 = System.nanoTime();
            solver.step(DT, SWEEPS);
            long t1 = System.nanoTime();

            insertion = ins;
            power = solver.power();
            peak = solver.peakFission();
            stepMs = (t1 - t0) / 1e6;
            simSeconds += DT;
            double wall = (System.nanoTime() - wallStart) / 1e9;
            realTimeFactor = wall > 0 ? simSeconds / wall : 1.0;
        }
    }

    // --------------------------------------------------------- ReactorModel

    /** No-op: the physics thread owns the stepping, not the server tick. */
    @Override
    public void step(double dt) {
        // intentionally empty — the background thread advances the core
    }

    @Override public double power() { return power; }
    @Override public double reactivity() { return 0; }
    @Override public double insertion() { return insertion; }
    @Override public double voidFraction() { return voidFraction; }
    @Override public double fuelTempC() { return fuelTempC; }
    @Override public double xenonRatio() { return xenon; }

    @Override
    public void setRodTarget(double target) {
        this.autoRod = false;
        this.insertionTarget = clamp01(target);
    }

    @Override
    public void setAutoRegulator(boolean on) {
        this.autoRod = on;
        this.powerSetpoint = power;
    }

    // ------------------------------------------------------------- readouts

    /** Largest local fission rate — the hot spot. */
    public double peakFission() { return peak; }

    /** Simulated seconds the core has advanced. */
    public double simSeconds() { return simSeconds; }

    /** Milliseconds the last 1 ms physics step took. */
    public double stepMs() { return stepMs; }

    /** Simulated seconds per wall second (>1 = faster than real time). */
    public double realTimeFactor() { return realTimeFactor; }

    @Override
    public void close() {
        running = false;
        try {
            thread.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }
}
