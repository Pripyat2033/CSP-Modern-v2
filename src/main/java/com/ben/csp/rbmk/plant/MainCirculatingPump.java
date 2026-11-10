package com.ben.csp.rbmk.plant;

/**
 * A main circulating pump (ГЦН, GTSN): one of the eight that push coolant through
 * the core. Each is a separate machine with its own state — running, tripped, or
 * coasting down on its flywheel — and the plant has to know about every one,
 * because losing two on one side was a reactor trip (docs/RBMK-PLANT-SYSTEMS.md §2).
 *
 * <p>Flow follows the pump curve: a little less than rated when the core pushes
 * back, and the real throttling to 6,000-7,000 m3/h below 500 MWt. On a trip the
 * flywheel coasts the flow down over several seconds — the rundown that, after
 * Chernobyl, was arranged to drive the ECCS pumps.
 */
public final class MainCirculatingPump {

    private final int index;
    private boolean running = true;
    /** Flow as a fraction of rated, 0..1. */
    private double flowFraction = 1.0;
    /** True while coasting down after a trip. */
    private boolean coasting = false;

    public MainCirculatingPump(int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }

    public boolean running() {
        return running;
    }

    public boolean coasting() {
        return coasting;
    }

    /** Volumetric flow this pump is delivering, m3/s. */
    public double flowM3s() {
        return flowFraction * PlantPhysics.MCP_FLOW_RATED_M3S;
    }

    /** Throttle to the reduced flow used below 500 MWt (a control valve closes). */
    public void setThrottled(boolean throttled) {
        // handled in step via the demand; kept explicit for the operator interface
        this.throttleDemand = throttled ? 0.0 : 1.0;
    }

    private double throttleDemand = 1.0;

    /** Trip the pump: the motor is off and the flywheel coasts it down. */
    public void trip() {
        running = false;
        coasting = true;
    }

    /** Restart the pump (motor re-energised, spins up quickly). */
    public void start() {
        running = true;
        coasting = false;
    }

    /**
     * Advance the pump by dt seconds.
     *
     * @param corePowerFraction reactor power, to decide throttling
     */
    public void step(double dt, double corePowerFraction) {
        double target;
        if (running) {
            target = (corePowerFraction < 0.156) // below 500 MWt / 3200 MWt
                ? PlantPhysics.MCP_FLOW_THROTTLED_M3S / PlantPhysics.MCP_FLOW_RATED_M3S
                : throttleDemand;
        } else if (coasting) {
            target = 0.0;
            flowFraction *= Math.exp(-dt / PlantPhysics.MCP_COAST_TAU_S);
            if (flowFraction < 0.02) {
                flowFraction = 0.0;
                coasting = false;
            }
            return;
        } else {
            target = 0.0;
        }
        // spin-up / spin-down toward the target over ~2 s
        flowFraction += (target - flowFraction) * Math.min(1.0, dt / 2.0);
        flowFraction = Math.max(0, Math.min(1, flowFraction));
    }
}
