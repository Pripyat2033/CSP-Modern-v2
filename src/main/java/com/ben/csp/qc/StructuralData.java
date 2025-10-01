package com.ben.csp.qc;

/**
 * Holds the mechanical properties of a specific block in the world,
 * separate from its vanilla Minecraft state.
 */
public class StructuralData {
    public double health = 1.0; // Intrinsic strength (0.0 to 1.0)
    public double load = 0.0;   // Current load from blocks above
    public boolean needsRecalculation = true;
}