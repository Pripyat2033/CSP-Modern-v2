package com.ben.csp.rbmk.plant;

/**
 * A steam condenser: rejects the turbine's waste heat to the cooling water. It
 * is where most of the reactor's 3,200 MWt finally goes — the reason the plant
 * needs an engineered cooling pond and a river (docs/RBMK-PLANT-SYSTEMS.md §5).
 */
public final class Condenser {

    private final int index;
    private double heatRejectedW;
    private double coolingWaterOutC = 25.0;

    public Condenser(int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }

    public double heatRejectedW() {
        return heatRejectedW;
    }

    public double coolingWaterOutC() {
        return coolingWaterOutC;
    }

    /**
     * Advance the condenser by dt.
     *
     * @param steamInKgS steam condensed this step (kg/s)
     * @param workExtractedW electrical work the turbine took from that steam (W)
     */
    public void step(double dt, double steamInKgS, double workExtractedW) {
        // the steam carries the heat the reactor put into it (relative to feedwater);
        // the condenser rejects what the turbine did not turn into electricity
        double heatIn = steamInKgS * (PlantPhysics.H_STEAM - PlantPhysics.H_FEEDWATER);
        heatRejectedW = Math.max(0, heatIn - workExtractedW);
        // cooling water temperature rise = Q / (m_dot * cp)
        double dT = heatRejectedW / (PlantPhysics.COND_COOLING_KGS * PlantPhysics.CP_WATER);
        coolingWaterOutC = 25.0 + dT;
    }
}
