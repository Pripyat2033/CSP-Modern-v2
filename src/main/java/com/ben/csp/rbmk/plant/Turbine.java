package com.ben.csp.rbmk.plant;

/**
 * A 500 MWe turbogenerator: the K-500-65/3000, one of two per unit. It is a
 * separate machine from the reactor — it converts the steam's heat to electricity
 * at its own efficiency, and its speed and power are its own state.
 *
 * <p>This is also where the accident's last act happened: when the turbines were
 * run down during the test, their rundown was supposed to carry the pumps. The
 * turbine's coasting behaviour is therefore modelled, not assumed away.
 */
public final class Turbine {

    private final int index;
    /** Steam the turbine is taking (kg/s). */
    private double steamInKgS;
    /** Electrical output (W). */
    private double powerW;
    /** Rotor speed as a fraction of 3,000 rpm. */
    private double speed = 1.0;
    private boolean tripped = false;

    public Turbine(int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }

    public double steamInKgS() {
        return steamInKgS;
    }

    public double powerW() {
        return powerW;
    }

    public double speedFraction() {
        return speed;
    }

    public double rpm() {
        return speed * PlantPhysics.TURBINE_RPM;
    }

    public boolean tripped() {
        return tripped;
    }

    public void trip() {
        tripped = true;
    }

    /**
     * Advance the turbine by dt.
     *
     * @param availableKgS steam the drums can supply (kg/s)
     */
    public void step(double dt, double availableKgS) {
        double demand = PlantPhysics.steamFlowKgS(1.0) / PlantPhysics.TURBINE_COUNT;
        if (tripped) {
            steamInKgS *= Math.exp(-dt / PlantPhysics.TURBINE_COAST_TAU_S);
            speed *= Math.exp(-dt / PlantPhysics.TURBINE_COAST_TAU_S);
        } else {
            steamInKgS = Math.min(availableKgS, demand);
            // the rotor holds speed on the grid; a little droop with steam loss
            double target = Math.min(1.0, steamInKgS / demand);
            speed += (target - speed) * Math.min(1.0, dt / 3.0);
        }
        // power follows the steam actually expanded, at the cycle's specific work
        powerW = Math.max(0, steamInKgS * PlantPhysics.TURBINE_SPECIFIC_WORK_J_PER_KG);
    }
}
