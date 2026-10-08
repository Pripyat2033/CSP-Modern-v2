package com.ben.csp.util;

/**
 * Science Grade: A utility providing water/steam properties for the RBMK simulation.
 * These approximations are optimized for the operating range of an RBMK-1000 (approx. 7 MPa).
 */
public class SteamTableUtil {

    /**
     * Returns the saturation temperature for a given pressure.
     * @param pressureMpa Pressure in Megapascals.
     * @return Saturation temperature in Celsius.
     */
    public static double getSaturationTemperature(double pressureMpa) {
        // Polynomial approximation for Psat around 7MPa (285.8C)
        return 179.91 * Math.pow(pressureMpa, 0.236);
    }

    /**
     * Returns the specific enthalpy of saturated liquid (hf).
     * @param pressureMpa Pressure in Megapascals.
     * @return Enthalpy in kJ/kg.
     */
    public static double getSaturatedLiquidEnthalpy(double pressureMpa) {
        // Linearized approximation for hf at saturation
        double tSat = getSaturationTemperature(pressureMpa);
        return tSat * (4.18 + 0.0015 * tSat);
    }

    /**
     * Returns the latent heat of vaporization (hfg) at a given pressure.
     * @param pressureMpa Pressure in Megapascals.
     * @return Latent heat in kJ/kg.
     */
    public static double getLatentHeat(double pressureMpa) {
        // Latent heat decreases as pressure/temperature approach the critical point.
        // Approximation for the RBMK operating range.
        return 2100.0 - (70.0 * pressureMpa);
    }

    /**
     * Returns the specific enthalpy of liquid water at a given temperature.
     * @param temperatureC Temperature in Celsius.
     * @return Enthalpy in kJ/kg.
     */
    public static double getLiquidEnthalpy(double temperatureC) {
        // h = cp * T. For SGB, we account for the slight non-linearity of cp.
        return temperatureC * (4.187 + 0.0001 * temperatureC);
    }
    
    public static double getSpecificHeat(double temperatureC) {
        // Simplified cp function
        return 4.187 + 0.0001 * temperatureC;
    }
}