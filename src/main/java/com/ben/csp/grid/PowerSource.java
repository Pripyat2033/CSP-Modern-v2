package com.ben.csp.grid;

/** Something that feeds the grid, such as a generating unit. */
public interface PowerSource {
    String id();

    /** The power it can supply right now, in megawatts. */
    double availableMw();
}
