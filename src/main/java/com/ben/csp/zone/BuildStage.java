package com.ben.csp.zone;

/**
 * How far a facility has been built. It starts as a marker on the ground — set
 * out, but nothing raised — and becomes a building as work is done on it.
 */
public enum BuildStage {
    STAKED("Разбивка — set out, marker only"),
    FOUNDATION("Foundation"),
    STRUCTURE("Structure"),
    ROOFED("Roofed"),
    FINISHED("Building complete");

    private final String label;

    BuildStage(String label) { this.label = label; }

    public String label() { return label; }

    /** True once the facility is a standing building. */
    public boolean isBuilding() { return this == FINISHED; }
}
