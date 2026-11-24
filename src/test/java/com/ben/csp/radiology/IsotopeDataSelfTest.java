package com.ben.csp.radiology;

/**
 * Self-test for REKORD layer 1: the nuclide decay data.
 *
 * <p>The point of this test is not that the loader returns numbers -- it is that
 * those numbers are the RIGHT numbers. Every check here compares a value loaded
 * from the generated data file against an independent published value that is
 * written into this test from the literature, not read from the same file. If
 * the fetcher, the API or the parse ever drifts, these checks fail.
 *
 * <p>What is deliberately NOT tested here: activity, dose rate, release
 * fractions. Those depend on inventory and transport, which are later layers.
 * A green run of this test says the nuclear data is right, and says nothing
 * about whether a source term has been modelled.
 *
 * <p>Run: {@code java -cp <out>:<gson>:src/main/resources ...IsotopeDataSelfTest}
 */
public final class IsotopeDataSelfTest {

    private static int checks = 0;
    private static int failures = 0;

    public static void main(String[] args) {
        System.out.println("=== IsotopeDataSelfTest (REKORD layer 1) ===");
        testLoadsCleanly();
        testHalfLives();
        testKnownGammaLines();
        testDecayConstants();
        testSpecificActivity();
        testDecayChains();
        testStableNuclides();
        testIsomers();
        testReleaseGroups();
        testNoDoubleCountedLines();
        System.out.printf("%nIsotopeDataSelfTest: %d checks, %d failures%n", checks, failures);
        if (failures > 0) {
            throw new AssertionError(failures + " check(s) failed");
        }
    }

    private static Isotope is(String symbol) {
        return IsotopeLoader.require(symbol);
    }

    /** The data file must load with no rejected records. */
    private static void testLoadsCleanly() {
        int n = IsotopeLoader.load();
        check("data file loads at least 50 nuclides", n >= 50, "n=" + n);
        check("no isotope record was rejected", IsotopeLoader.problems().isEmpty(),
            String.valueOf(IsotopeLoader.problems()));
    }

    /**
     * Half-lives in seconds against published values (ENSDF-evaluated, quoted in
     * years/days/hours as the literature gives them). Tolerance is 0.5% except
     * where the published figure is itself rounded to a few digits.
     */
    private static void testHalfLives() {
        double year = 365.2422 * 86400.0;
        check("Cs-137 T1/2 = 30.08 y", rel(is("Cs137").halfLifeSeconds(), 30.08 * year, 0.005),
            String.format("%.4g s", is("Cs137").halfLifeSeconds()));
        check("I-131 T1/2 = 8.0252 d", rel(is("I131").halfLifeSeconds(), 8.0252 * 86400, 0.005),
            String.format("%.4g s", is("I131").halfLifeSeconds()));
        check("Sr-90 T1/2 = 28.79 y", rel(is("Sr90").halfLifeSeconds(), 28.79 * year, 0.005),
            String.format("%.4g s", is("Sr90").halfLifeSeconds()));
        check("Co-60 T1/2 = 1925.28 d",
            rel(is("Co60").halfLifeSeconds(), 1925.28 * 86400, 0.005),
            String.format("%.4g s", is("Co60").halfLifeSeconds()));
        check("H-3 T1/2 = 12.32 y", rel(is("H3").halfLifeSeconds(), 12.32 * year, 0.005),
            String.format("%.4g s", is("H3").halfLifeSeconds()));
        check("Pu-239 T1/2 = 24110 y",
            rel(is("Pu239").halfLifeSeconds(), 24110 * year, 0.005),
            String.format("%.4g s", is("Pu239").halfLifeSeconds()));
        check("Xe-135 T1/2 = 9.14 h", rel(is("Xe135").halfLifeSeconds(), 9.14 * 3600, 0.005),
            String.format("%.4g s", is("Xe135").halfLifeSeconds()));
        // ENSDF evaluates 2.825 h. The commonly quoted 2.84 h is that figure
        // rounded, so the reference here is the evaluated one.
        check("Kr-88 T1/2 = 2.825 h", rel(is("Kr88").halfLifeSeconds(), 2.825 * 3600, 0.005),
            String.format("%.4g s", is("Kr88").halfLifeSeconds()));
        check("Ru-106 T1/2 = 371.8 d",
            rel(is("Ru106").halfLifeSeconds(), 371.8 * 86400, 0.01),
            String.format("%.4g s", is("Ru106").halfLifeSeconds()));
        check("Ce-144 T1/2 = 285.0 d",
            rel(is("Ce144").halfLifeSeconds(), 285.0 * 86400, 0.01),
            String.format("%.4g s", is("Ce144").halfLifeSeconds()));
    }

    /** The gamma lines that matter for dose, with their published intensities. */
    private static void testKnownGammaLines() {
        check("Cs-137 gamma at 661.657 keV, 85.1% per decay",
            gamma(is("Cs137"), 661.657, 0.851, 0.002),
            gammaDetail(is("Cs137"), 661.657));
        check("Co-60 gamma at 1173.228 keV, 99.85% per decay",
            gamma(is("Co60"), 1173.228, 0.9985, 0.002),
            gammaDetail(is("Co60"), 1173.228));
        check("Co-60 gamma at 1332.492 keV, 99.98% per decay",
            gamma(is("Co60"), 1332.492, 0.9998, 0.002),
            gammaDetail(is("Co60"), 1332.492));
        check("I-131 gamma at 364.489 keV, 81.5% per decay",
            gamma(is("I131"), 364.489, 0.815, 0.003),
            gammaDetail(is("I131"), 364.489));
        check("Cs-134 gamma at 604.721 keV, 97.6% per decay",
            gamma(is("Cs134"), 604.721, 0.976, 0.003),
            gammaDetail(is("Cs134"), 604.721));
        check("Xe-135 gamma at 249.794 keV, 90.0% per decay",
            gamma(is("Xe135"), 249.794, 0.90, 0.003),
            gammaDetail(is("Xe135"), 249.794));
    }

    /** lambda = ln2 / T: Cs-137 is 7.30e-10 s^-1, the number used in fallout work. */
    private static void testDecayConstants() {
        check("Cs-137 decay constant = 7.30e-10 s^-1",
            rel(is("Cs137").decayConstant(), 7.30e-10, 0.003),
            String.format("%.4e s^-1", is("Cs137").decayConstant()));
        check("I-131 decay constant = 9.98e-7 s^-1",
            rel(is("I131").decayConstant(), 9.98e-7, 0.005),
            String.format("%.4e s^-1", is("I131").decayConstant()));
    }

    /** Specific activity: Cs-137 is 3.2 TBq/g, the textbook figure. */
    private static void testSpecificActivity() {
        double cs = is("Cs137").specificActivityBqPerGram();
        check("Cs-137 specific activity = 3.2 TBq/g",
            cs > 3.15e12 && cs < 3.30e12, String.format("%.3e Bq/g", cs));
        // Co-60 is 1130 Ci/g = 41.8 TBq/g. (An earlier draft of this test said
        // 4.1 TBq/g -- a factor-of-ten slip in the reference, not in the data.)
        double co = is("Co60").specificActivityBqPerGram();
        check("Co-60 specific activity = 41.8 TBq/g (1130 Ci/g)",
            co > 4.10e13 && co < 4.25e13, String.format("%.3e Bq/g", co));
    }

    /** Decay daughters, from the evaluated decay scheme. */
    private static void testDecayChains() {
        check("Cs-137 decays to Ba-137", hasDaughter(is("Cs137"), "Ba137"),
            daughterList(is("Cs137")));
        check("Sr-90 decays to Y-90", hasDaughter(is("Sr90"), "Y90"),
            daughterList(is("Sr90")));
        check("I-131 decays to Xe-131", hasDaughter(is("I131"), "Xe131"),
            daughterList(is("I131")));
        check("Xe-135 decays to Cs-135", hasDaughter(is("Xe135"), "Cs135"),
            daughterList(is("Xe135")));
        check("Pu-241 decays to Am-241 by beta, and to U-237 by alpha",
            hasDaughter(is("Pu241"), "Am241") && hasDaughter(is("Pu241"), "U237"),
            daughterList(is("Pu241")));
    }

    /** Stable nuclides must be flagged, not given a fake half-life. */
    private static void testStableNuclides() {
        check("Sm-149 is stable", is("Sm149").stable(), "stable=" + is("Sm149").stable());
        check("a stable nuclide has zero decay constant",
            is("Sm149").decayConstant() == 0.0,
            String.valueOf(is("Sm149").decayConstant()));
    }

    /** Isomers must carry the ISOMER half-life, not the ground state's. */
    private static void testIsomers() {
        check("Tc-99m T1/2 = 6.0067 h (not Tc-99's 211000 y)",
            rel(is("Tc99m").halfLifeSeconds(), 6.0067 * 3600, 0.005),
            String.format("%.4g s", is("Tc99m").halfLifeSeconds()));
        check("Tc-99m is marked as an isomer", is("Tc99m").isomer(),
            "isomer=" + is("Tc99m").isomer());
        check("Tc-99m half-life is nowhere near Tc-99's",
            is("Tc99m").halfLifeSeconds() < is("Tc99").halfLifeSeconds() / 1000.0,
            String.format("m=%.3g g=%.3g", is("Tc99m").halfLifeSeconds(),
                is("Tc99").halfLifeSeconds()));
        check("Ag-110m T1/2 = 249.8 d",
            rel(is("Ag110m").halfLifeSeconds(), 249.8 * 86400, 0.005),
            String.format("%.4g s", is("Ag110m").halfLifeSeconds()));
        check("Xe-131m T1/2 = 11.84 d",
            rel(is("Xe131m").halfLifeSeconds(), 11.84 * 86400, 0.005),
            String.format("%.4g s", is("Xe131m").halfLifeSeconds()));
    }

    /** Every isotope must carry a release group; the contamination model needs it. */
    private static void testReleaseGroups() {
        boolean allGrouped = true;
        for (Isotope i : IsotopeRegistry.getInstance().all()) {
            if (i.group() == null || i.group().isEmpty() || i.group().equals("unspecified")) {
                allGrouped = false;
                System.out.println("    ungrouped: " + i.symbol());
            }
        }
        check("every isotope carries a release group", allGrouped, "");
        check("noble gases include Xe-133 and Kr-88",
            grouped("noble_gas", "Xe133") && grouped("noble_gas", "Kr88"), "");
        check("volatiles include I-131 and Cs-137",
            grouped("volatile", "I131") && grouped("volatile", "Cs137"), "");
    }

    /**
     * No emission may appear twice at the same energy within one isotope. A
     * duplicated line silently doubles its contribution to dose, and the
     * evaluated file does repeat some transitions, so the merge has to hold.
     */
    private static void testNoDoubleCountedLines() {
        int duplicated = 0;
        for (Isotope i : IsotopeRegistry.getInstance().all()) {
            java.util.Set<String> seen = new java.util.HashSet<>();
            for (Isotope.Emission e : i.emissions()) {
                String key = e.type() + "@" + Math.round(e.energyMeV() * 1e6);
                if (!seen.add(key)) {
                    duplicated++;
                    System.out.println("    duplicate " + i.symbol() + " " + key);
                }
            }
        }
        check("no emission line is counted twice", duplicated == 0,
            duplicated + " duplicates");
    }

    private static boolean grouped(String group, String symbol) {
        Isotope i = IsotopeRegistry.getInstance().getIsotope(symbol);
        return i != null && group.equals(i.group());
    }

    private static boolean hasDaughter(Isotope i, String symbol) {
        for (Isotope.DecayProduct p : i.decayProducts()) {
            if (p.symbol().equals(symbol)) {
                return true;
            }
        }
        return false;
    }

    private static String daughterList(Isotope i) {
        StringBuilder sb = new StringBuilder();
        for (Isotope.DecayProduct p : i.decayProducts()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(p.symbol());
        }
        return sb.toString();
    }

    /** True if a gamma line exists within 0.2 keV of `energyKeV` with the intensity. */
    private static boolean gamma(Isotope i, double energyKeV, double intensity, double tol) {
        for (Isotope.Emission e : i.emissions()) {
            if (!e.type().equals("GAMMA")) {
                continue;
            }
            if (Math.abs(e.energyMeV() * 1000.0 - energyKeV) < 0.2) {
                return Math.abs(e.intensity() - intensity) <= tol;
            }
        }
        return false;
    }

    private static String gammaDetail(Isotope i, double energyKeV) {
        for (Isotope.Emission e : i.emissions()) {
            if (e.type().equals("GAMMA")
                && Math.abs(e.energyMeV() * 1000.0 - energyKeV) < 0.2) {
                return String.format("%.3f keV at %.4f", e.energyMeV() * 1000.0,
                    e.intensity());
            }
        }
        return "line absent";
    }

    private static boolean rel(double actual, double expected, double tol) {
        return expected != 0.0 && Math.abs(actual - expected) / Math.abs(expected) <= tol;
    }

    private static void check(String what, boolean ok, String detail) {
        checks++;
        if (!ok) {
            failures++;
        }
        System.out.printf("%s %s%s%n", ok ? "PASS" : "FAIL", what,
            detail == null || detail.isEmpty() ? "" : "  [" + detail + "]");
    }
}
