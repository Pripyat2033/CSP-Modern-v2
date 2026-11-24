package com.ben.csp.grid;

/**
 * A load inside the plant: a motor, a heater, a panel supply. It draws power
 * when its bus is live, and it knows what losing power does to it.
 *
 * <p>This is the interface that closes the loop the plant was missing. The main
 * circulating pumps are {@code PlantLoad}s at 6 MW each; when their 6 kV bus goes
 * dark they stop being driven and coast down on their flywheels, which is the
 * physical event the whole accident turns on.
 */
public interface PlantLoad {

    String id();

    /** Power drawn while energised, in megawatts. */
    double demandMw();

    /**
     * Installed rating, in megawatts, whether or not the load is running. This is
     * what the bus is sized for; {@link #demandMw()} is what it is actually
     * drawing. Confusing the two makes a dead bus look like an empty one.
     */
    double ratedMw();

    SupplyClass supplyClass();

    boolean isEnergised();

    /**
     * Tell the load whether its supply is live. The load decides what that means:
     * a pump coasts, a heater cools, a panel goes dark.
     */
    void setEnergised(boolean energised);
}
