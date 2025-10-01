package com.ben.csp.radiology;

/**
 * Defines the Half-Value Layer (HVL) for a material against different radiation types.
 * HVL is the thickness in meters required to reduce radiation intensity by 50%.
 */
public record ShieldingProperties(double gammaHVL, double neutronHVL) {
}