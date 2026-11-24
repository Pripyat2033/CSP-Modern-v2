package com.ben.csp.grid;

/**
 * An emergency diesel generator (ДГ). Three per unit at 5.5 MW each, on automatic
 * start within 15 s of losing the essential boards.
 *
 * <p>Two things about diesels are load-bearing and are modelled rather than
 * assumed away. The first is the delay: they are not instantaneous, and in the
 * fifteen seconds before they take load the essential boards are dead. The second
 * is the capacity: 3 x 5.5 MW is 16.5 MW. The eight main circulating pumps are
 * 36 MW, so the diesels cannot carry the main pumps, and no amount of waiting
 * makes them able to. That is not a modelling limitation — it is the reason a
 * station blackout at an RBMK is unrecoverable.
 */
public final class DieselGenerator implements PowerSource {

    /** Seconds from start signal to taking load (auto-start < 15 s). */
    public static final double START_DELAY_S = 15.0;

    private final String id;
    private final double ratedMw;
    /** Seconds since the start signal; negative means not started. */
    private double sinceStartS = -1;
    private boolean running;

    public DieselGenerator(String id, double ratedMw) {
        this.id = id;
        this.ratedMw = ratedMw;
    }

    @Override public String id() { return id; }
    public double ratedMw() { return ratedMw; }
    public boolean isRunning() { return running; }
    /** True while cranking and not yet able to carry load. */
    public boolean isStarting() { return sinceStartS >= 0 && !running; }
    public double sinceStartS() { return sinceStartS; }

    /** The essential boards lost voltage: start cranking. */
    public void startSignal() {
        if (!running && sinceStartS < 0) {
            sinceStartS = 0;
        }
    }

    /** Operator stops the set. */
    public void stop() {
        running = false;
        sinceStartS = -1;
    }

    public void step(double dt) {
        if (sinceStartS < 0 || running) {
            return;
        }
        sinceStartS += dt;
        if (sinceStartS >= START_DELAY_S) {
            running = true;
        }
    }

    @Override
    public double availableMw() {
        return running ? ratedMw : 0;
    }

    @Override
    public String toString() {
        String state = running ? "running"
            : sinceStartS >= 0 ? String.format("starting %.1f/%.0f s", sinceStartS, START_DELAY_S)
            : "stopped";
        return String.format("%s %.1f MW [%s]", id, ratedMw, state);
    }
}
