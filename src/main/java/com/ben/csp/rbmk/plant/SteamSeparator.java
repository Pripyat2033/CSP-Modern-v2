package com.ben.csp.rbmk.plant;

/**
 * A steam separator drum (барабан-сепаратор): one of the four thick horizontal
 * drums above the reactor where the steam-water mixture from the core is split
 * into dry steam (to the turbines) and water (back to the pumps).
 *
 * <p>Each drum is a real vessel with a water level that rises and falls with the
 * balance of inflow (from the core) and outflow (steam to the turbines, water to
 * the pumps, feedwater in). The level is the quantity the operators watch, and
 * losing it — high or low — was a reactor trip (docs/RBMK-PLANT-SYSTEMS.md §3).
 */
public final class SteamSeparator {

    private final int index;
    /** Water volume as a fraction of the drum, 0..1. */
    private double level = PlantPhysics.SEPARATOR_NOMINAL_FILL;
    /** Steam pressure (Pa); the drum sets the whole circuit pressure. */
    private double pressurePa = PlantPhysics.P_SYSTEM_PA;
    /** Steam mass flow out to the turbine (kg/s). */
    private double steamOutKgS;
    /** Water mass flow down to the pumps (kg/s). */
    private double waterOutKgS;
    /** Feedwater mass flow in (kg/s). */
    private double feedwaterInKgS;

    public SteamSeparator(int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }

    public double level() {
        return level;
    }

    public double pressurePa() {
        return pressurePa;
    }

    public double steamOutKgS() {
        return steamOutKgS;
    }

    /**
     * Advance the drum by dt seconds.
     *
     * @param steamInKgS   steam-water mixture arriving from the core (kg/s)
     * @param quality      steam quality of that mixture (~0.15)
     * @param demandKgS    steam the turbine is asking for (kg/s)
     * @param feedwaterKgS feedwater being pumped in (kg/s)
     */
    public void step(double dt, double steamInKgS, double quality, double demandKgS,
                     double feedwaterKgS) {
        // steam is whatever the drum can give up to the demand
        steamOutKgS = Math.min(demandKgS, steamInKgS * quality);
        // the water that did not boil returns to the pumps
        waterOutKgS = steamInKgS * (1 - quality);
        feedwaterInKgS = feedwaterKgS;

        // drum mass balance. The circulating water (in from the core, out to the
        // pumps) cancels, so the level floats on the net of feedwater in and steam
        // out — the balance the operators actually watch.
        double drumMassKg = PlantPhysics.SEPARATOR_AREA_M2 * PlantPhysics.SEPARATOR_LENGTH_M
            * PlantPhysics.RHO_COOLANT;
        double dMass = (feedwaterKgS - steamOutKgS) * dt;
        level += dMass / drumMassKg;
        level = Math.max(0, Math.min(1, level));

        // pressure drifts toward the system setpoint
        double targetPa = PlantPhysics.P_SYSTEM_PA;
        pressurePa += (targetPa - pressurePa) * Math.min(1, dt / 10.0);
    }

    public boolean highLevelTrip() {
        return level >= PlantPhysics.SEPARATOR_TRIP_HIGH_FILL;
    }

    public boolean lowLevelTrip() {
        return level <= PlantPhysics.SEPARATOR_TRIP_LOW_FILL;
    }
}
