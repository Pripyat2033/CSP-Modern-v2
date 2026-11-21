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

    /**
     * The АЗ-1 feedwater protection compares the live flow against what it recently
     * was: «снижение расхода питательной воды до 0,75 текущего» — to 0.75 of the
     * CURRENT flow, i.e. a rate-of-drop protection, not an absolute threshold. So the
     * reference is the highest flow seen in the last {@link #REFERENCE_WINDOW_S}
     * seconds, held as a small timestamped history. A first-order average does NOT
     * work here: it follows the drop down and the protection never fires (measured —
     * a 40 % drop to 900 kg/s was missed with a 60 s lag).
     */
    private final java.util.ArrayDeque<double[]> flowHistory = new java.util.ArrayDeque<>();
    private double simSeconds = 0;
    private static final double REFERENCE_WINDOW_S = 60.0;

    /** True when feedwater flow has fallen to 0.75 of its recent value (АЗ-1). */
    public boolean lowFlowTrip() {
        return flowKgS < PlantPhysics.FEEDWATER_TRIP_FRACTION * referenceFlowKgS();
    }

    /** The baseline the АЗ-1 feedwater protection is comparing against, kg/s. */
    public double referenceFlowKgS() {
        double max = flowKgS;
        for (double[] e : flowHistory) {
            if (e[1] > max) {
                max = e[1];
            }
        }
        return max;
    }

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

        // the АЗ-1 reference: a trailing 60 s window of flow, so a drop is measured
        // against what the flow recently WAS and a slow ramp does not trip it
        simSeconds += dt;
        flowHistory.addLast(new double[] {simSeconds, flowKgS});
        while (!flowHistory.isEmpty()
                && flowHistory.peekFirst()[0] < simSeconds - REFERENCE_WINDOW_S) {
            flowHistory.removeFirst();
        }
    }
}
