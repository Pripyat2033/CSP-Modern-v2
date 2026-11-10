package com.ben.csp.rbmk.plant;

/**
 * The feedwater and condensate train: condenser → condensate pumps → deaerators →
 * feedwater pumps → the steam separator drums. The deaerators are both the water
 * treatment and the plant's water reservoir, so the drum level is ultimately set
 * here (docs/RBMK-PLANT-SYSTEMS.md §5).
 *
 * <p>Feedwater enters at 155-165 °C, much colder than the 284 °C water returning
 * from the drums, and this is physically important: at low power the cold
 * feedwater is a larger fraction of the core inlet, which raises the inlet
 * temperature and is a real reactivity concern.
 */
public final class FeedwaterSystem {

    /** Feedwater flow (kg/s), the control output. */
    private double flowKgS = PlantPhysics.MFLOW_FEEDWATER_KGS;
    /** Deaerator storage level, 0..1. */
    private double deaeratorLevel = 0.6;
    /** Feedwater temperature (C). */
    private double temperatureC = PlantPhysics.T_FEEDWATER_C;

    public double flowKgS() {
        return flowKgS;
    }

    public double deaeratorLevel() {
        return deaeratorLevel;
    }

    public double temperatureC() {
        return temperatureC;
    }

    /**
     * Advance the feedwater train by dt.
     *
     * @param condensateInKgS  condensate returning from the condensers (kg/s)
     * @param demandKgS        feedwater the drums need to hold level (kg/s)
     */
    public void step(double dt, double condensateInKgS, double demandKgS) {
        // deaerator holds the balance of condensate in and feedwater out
        double reservoirKg = PlantPhysics.DEAERATOR_COUNT * 500e3; // ~500 t each
        deaeratorLevel += (condensateInKgS - flowKgS) * dt / reservoirKg;
        deaeratorLevel = Math.max(0, Math.min(1, deaeratorLevel));

        // feedwater pump tracks demand, limited by what the deaerators hold
        double achievable = deaeratorLevel > 0.05 ? demandKgS : 0.0;
        flowKgS += (achievable - flowKgS) * Math.min(1.0, dt / 5.0);

        // temperature drifts toward the feedwater setpoint
        temperatureC += (PlantPhysics.T_FEEDWATER_C - temperatureC) * Math.min(1.0, dt / 20.0);
    }
}
