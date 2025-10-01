package com.ben.csp.radiology;

/**
 * ARL: Defines how contamination interacts with a block surface.
 * @param retentionFactor How "sticky" the surface is (0.0 = frictionless, 1.0 = flypaper).
 * @param washoutFactor How easily rain washes contamination away (0.0 = waterproof, 1.0 = dissolves).
 * @param permeability How much contamination seeps into the block below (0.0 = impermeable, 1.0 = porous).
 */
public record DepositionProperties(double retentionFactor, double washoutFactor, double permeability) {
}