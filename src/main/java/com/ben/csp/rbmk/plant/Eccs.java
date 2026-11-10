package com.ben.csp.rbmk.plant;

/**
 * The emergency core cooling system (САОР / ECCS): twelve accumulator tanks at
 * 10 MPa and the long-term pumps. Separate from the main circuit, and its own
 * system with its own state (docs/RBMK-PLANT-SYSTEMS.md §6).
 */
public final class Eccs {

    public static final int ACCUMULATORS = 12;
    public static final double ACCUMULATOR_PRESSURE_PA = 10.0e6;
    /** Water inventory per accumulator (kg) — sized to give ~100 s of cooling. */
    public static final double ACCUMULATOR_WATER_KG = 25e3;

    private final double[] accumulatorWaterKg = new double[ACCUMULATORS];
    private boolean actuated = false;
    private boolean longTermPumpsRunning = false;
    private double injectionKgS;

    public Eccs() {
        for (int i = 0; i < ACCUMULATORS; i++) {
            accumulatorWaterKg[i] = ACCUMULATOR_WATER_KG;
        }
    }

    public boolean actuated() {
        return actuated;
    }

    public boolean longTermPumpsRunning() {
        return longTermPumpsRunning;
    }

    public double injectionKgS() {
        return injectionKgS;
    }

    public double remainingWaterKg() {
        double s = 0;
        for (double w : accumulatorWaterKg) {
            s += w;
        }
        return s;
    }

    /** Fire the accumulators (the fast-acting valves open). */
    public void actuate() {
        this.actuated = true;
    }

    public void startLongTermPumps() {
        this.longTermPumpsRunning = true;
    }

    /** Advance the ECCS by dt: inject the accumulator water while it lasts. */
    public void step(double dt) {
        if (!actuated) {
            injectionKgS = 0;
            return;
        }
        // accumulators inject hard until empty, then the long-term pumps take over
        double perTankRate = ACCUMULATOR_WATER_KG / 100.0; // ~100 s of injection
        double total = 0;
        for (int i = 0; i < ACCUMULATORS; i++) {
            double injected = Math.min(accumulatorWaterKg[i], perTankRate * dt);
            accumulatorWaterKg[i] -= injected;
            total += injected;
        }
        double pumps = longTermPumpsRunning ? 200.0 : 0.0; // kg/s from the pools
        injectionKgS = total / dt + pumps;
    }
}
