package com.ben.csp.grid;

/**
 * How important a load is, which decides whether it is shed when the grid is
 * short. Lower ordinal is more important: ESSENTIAL is never shed.
 */
public enum LoadPriority {
    /** Safety and essential services: never shed. */
    ESSENTIAL("essential"),
    /** Important but shed as a last resort. */
    VITAL("vital"),
    /** Ordinary load. */
    NORMAL("normal"),
    /** Shed first. */
    SHEDDABLE("sheddable");

    private final String label;

    LoadPriority(String label) { this.label = label; }

    public String label() { return label; }

    /** True if this load may be shed, in the order given by the enum. */
    public boolean isSheddable() { return this != ESSENTIAL; }
}
