package com.ben.csp.grid;

/** Something on the grid that draws power, such as a building or a plant. */
public interface PowerConsumer {
    String id();

    /** The power it draws when energised, in megawatts. */
    double demandMw();

    LoadPriority priority();

    boolean isEnergised();

    void setEnergised(boolean energised);
}
