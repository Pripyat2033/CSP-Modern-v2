package com.ben.csp.radiology;

import java.util.List;
import java.util.Map;

/**
 * An immutable data record representing a single radionuclide, compliant with the ARL standard.
 * This class holds all the fundamental physical properties required for a full-fidelity simulation.
 *
 * <p>Every value comes from the evaluated ENSDF library via the IAEA Nuclear Data
 * Services API; the {@code source} and {@code ensdfReference} fields carry the
 * citation so any number can be checked against the evaluated file. See
 * {@link IsotopeLoader} and {@code tools/radiology/fetch_decay_data.py}.
 *
 * @param name The common name of the isotope (e.g., "Cs-137").
 * @param symbol The chemical symbol and mass number (e.g., "Cs137").
 * @param halfLifeSeconds The half-life of the isotope in seconds. A stable
 *        nuclide carries {@link Double#POSITIVE_INFINITY}, flagged by {@code stable}.
 * @param decayModes A map of decay modes (e.g., "B-") to their branching ratios (0.0 to 1.0).
 * @param decayProducts A list of resulting isotopes from decay.
 * @param emissions A list of particle emissions, including type (ALPHA, BETA, GAMMA) and energy in MeV.
 * @param group The release group the isotope was classified into (noble gas,
 *        volatile, semi-volatile, particulate, activation, process).
 * @param stable True if the nuclide has no half-life.
 * @param source The URL the record was fetched from.
 * @param ensdfReference ENSDF publication date and authors for the evaluation.
 * @param isomer True if this is a metastable state (Tc-99m), which carries its own
 *        half-life and must never be confused with the ground state.
 */
public record Isotope(
    String name,
    String symbol,
    double halfLifeSeconds,
    Map<String, Double> decayModes,
    List<DecayProduct> decayProducts,
    List<Emission> emissions,
    String group,
    boolean stable,
    String source,
    String ensdfReference,
    boolean isomer
) {
    /**
     * Represents a product of radioactive decay.
     * @param symbol The symbol of the resulting isotope (e.g., "Ba137").
     * @param branchingRatio The probability of this decay path (0.0 to 1.0); NaN
     *        when the evaluated modes could not be matched to a single branch.
     */
    public record DecayProduct(String symbol, double branchingRatio) {}

    /**
     * Represents a single particle or photon emission.
     * @param type The type of emission (e.g., "GAMMA", "BETA").
     * @param energyMeV The energy of the emission in Mega-electron Volts.
     * @param intensity The probability of this specific emission per decay event (0.0 to 1.0).
     */
    public record Emission(String type, double energyMeV, double intensity) {}

    /** Decay constant lambda = ln2 / T_half, in s^-1. Zero for a stable nuclide. */
    public double decayConstant() {
        return stable || halfLifeSeconds <= 0.0 || Double.isInfinite(halfLifeSeconds)
            ? 0.0
            : Math.log(2.0) / halfLifeSeconds;
    }

    /** Total emitted energy per decay for one emission type, in MeV. */
    public double energyPerDecayMeV(String type) {
        double sum = 0.0;
        for (Emission e : emissions) {
            if (e.type().equals(type)) {
                sum += e.energyMeV() * e.intensity();
            }
        }
        return sum;
    }

    /** Specific activity in Bq per gram, from lambda and the mass number. */
    public double specificActivityBqPerGram() {
        int mass = massNumber();
        if (mass <= 0 || stable) {
            return 0.0;
        }
        // A = lambda * N / m, with the number of atoms per gram N_A / M.
        return decayConstant() * 6.02214076e23 / mass;
    }

    /** Mass number parsed from the symbol ("Cs137" -> 137); 0 if unparseable. */
    public int massNumber() {
        int i = 0;
        while (i < symbol.length() && !Character.isDigit(symbol.charAt(i))) {
            i++;
        }
        if (i >= symbol.length()) {
            return 0;
        }
        try {
            return Integer.parseInt(symbol.substring(i));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
