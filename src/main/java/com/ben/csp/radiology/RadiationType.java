package com.ben.csp.radiology;

/**
 * Defines the types of radiation, each with its own physical properties.
 * @param rangeInAir The effective distance in meters the particle travels in air.
 * @param biologicalEffectiveness A multiplier for the damage this type of radiation does to tissue.
 */
public enum RadiationType {
    ALPHA(0.05, 20.0), // Very short range, easily stopped, but very damaging if ingested/inhaled.
    BETA(3.0, 1.0),   // Medium range, stopped by thin metal/plastic.
    GAMMA(500.0, 1.0),  // Long range, requires thick lead/concrete.
    NEUTRON(500.0, 10.0); // Long range, requires significant hydrogen-rich shielding (water, concrete).

    public final double rangeInAir;
    public final double biologicalEffectiveness;

    RadiationType(double rangeInAir, double biologicalEffectiveness) {
        this.rangeInAir = rangeInAir;
        this.biologicalEffectiveness = biologicalEffectiveness;
    }
}