package com.ben.csp.world;

public enum AtmosphericStability {
    UNSTABLE, // Daytime, promotes vertical mixing
    NEUTRAL,  // Transitional periods (dawn/dusk)
    STABLE    // Nighttime, traps plumes near the ground
}