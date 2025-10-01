package com.ben.csp.radiology;

import java.util.List;
import java.util.Map;

/**
 * An immutable data record representing a single radionuclide, compliant with the ARL standard.
 * This class holds all the fundamental physical properties required for a full-fidelity simulation.
 *
 * @param name The common name of the isotope (e.g., "Cesium-137").
 * @param symbol The chemical symbol and mass number (e.g., "137Cs").
 * @param halfLifeSeconds The half-life of the isotope in seconds.
 * @param decayModes A map of decay modes (e.g., "BETA") to their branching ratios (0.0 to 1.0).
 * @param decayProducts A list of resulting isotopes from decay.
 * @param emissions A list of particle emissions, including type (ALPHA, BETA, GAMMA) and energy in MeV.
 */
public record Isotope(
    String name,
    String symbol,
    double halfLifeSeconds,
    Map<String, Double> decayModes,
    List<DecayProduct> decayProducts,
    List<Emission> emissions
) {
    /**
     * Represents a product of radioactive decay.
     * @param symbol The symbol of the resulting isotope (e.g., "137Ba").
     * @param branchingRatio The probability of this decay path (0.0 to 1.0).
     */
    public record DecayProduct(String symbol, double branchingRatio) {}

    /**
     * Represents a single particle or photon emission.
     * @param type The type of emission (e.g., "GAMMA", "BETA").
     * @param energyMeV The energy of the emission in Mega-electron Volts.
     * @param intensity The probability of this specific emission per decay event (0.0 to 1.0).
     */
    public record Emission(String type, double energyMeV, double intensity) {}
}