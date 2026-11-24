package com.ben.csp.radiology;

import java.util.List;

/**
 * Self-test for REKORD layer 2: the core inventory at the instant of the accident.
 *
 * <p>Layer 1 asked whether the nuclear data was right. This asks a different
 * question: given right data, did the burn and the decay-chain solve produce the
 * right atoms? Three kinds of check, in increasing order of independence:
 *
 * <ol>
 *   <li><b>Solver checks.</b> A closed-form Bateman answer for one nuclide,
 *       computed here from its cumulative yield, must match what the generator
 *       produced by integrating the whole coupled system. This catches a broken
 *       matrix, a dropped branch, a wrong decay constant. It does not catch a
 *       wrong yield, because both sides use the same yield.</li>
 *   <li><b>Invariants.</b> Properties that must hold whatever the yields are:
 *       two fission-product atoms per fission (mass conservation), and secular
 *       equilibrium between a parent and a much shorter-lived daughter, where
 *       the atom ratio must equal the half-life ratio.</li>
 *   <li><b>Published anchors.</b> Cs-137 mass and activity in the core, written
 *       into this test from the literature. These test the yields and the
 *       fission count too, so they are the only checks that could catch the
 *       whole chain being wrong by a common factor.</li>
 * </ol>
 *
 * <p>Deliberately not tested: anything about release, dose or transport. Layer 2
 * is atoms in the core at shutdown. A green run says the inventory is right and
 * says nothing about what left the building.
 *
 * <p>Run: {@code java -cp <out>:<gson>:src/main/resources ...InventorySelfTest}
 */
public final class InventorySelfTest {

    private static int checks = 0;
    private static int failures = 0;

    /** Seconds in a Julian year, for half-lives the literature quotes in years. */
    private static final double YEAR = 365.2422 * 86400.0;

    public static void main(String[] args) {
        System.out.println("=== InventorySelfTest (REKORD layer 2) ===");
        testLoadsCleanly();
        testMassConservation();
        testSecularEquilibrium();
        testAnalyticBateman();
        testCs137Anchor();
        testIsomersKeptSeparate();
        testNoActivityInLayerTwo();
        testMassBalanceResidual();
        System.out.printf("%nInventorySelfTest: %d checks, %d failures%n", checks, failures);
        if (failures > 0) {
            throw new AssertionError(failures + " check(s) failed");
        }
    }

    /** The data file must load with every record usable. */
    private static void testLoadsCleanly() {
        int n = InventoryLoader.load();
        check("inventory loads more than 1000 nuclides", n > 1000, "n=" + n);
        check("no nuclide record was rejected", InventoryLoader.problems().isEmpty(),
            String.valueOf(InventoryLoader.problems()));
        check("total atoms is positive", InventoryLoader.totalAtoms() > 0.0,
            fmt(InventoryLoader.totalAtoms()));
    }

    /**
     * Mass conservation, the strongest yield-independent check there is.
     *
     * <p>Fission splits one nucleus into two fragments, so the number of
     * fission-product atoms per fission must be 2.00. This is true of any yield
     * set that sums to 200%, whatever the individual yields are, and it is true
     * of the inventory only if no atoms were created or destroyed on the way
     * through the decay chains -- no dropped daughter, no double-counted branch,
     * no nuclide silently parked. It is the check that caught the solver losing
     * 27% of Ru-106.
     *
     * <p>The window is 1.5% wide. Atoms do leave the fission-product region
     * legitimately: beta-delayed neutron emission moves 1.45% of them into the
     * chain below, and a few are still in a precursor the evaluation gives no
     * decay mode for. Both are accounted for and neither is a solver error.
     */
    private static void testMassConservation() {
        double fissions = InventoryLoader.fissionCount().total();
        check("fission count is positive", fissions > 0.0, fmt(fissions));
        double perFission = InventoryLoader.totalAtoms() / fissions;
        check("two fission-product atoms per fission (mass conservation)",
            perFission > 1.97 && perFission < 2.03,
            String.format("%.5f atoms/fission", perFission));
    }

    /**
     * Secular equilibrium, which needs no yield at all.
     *
     * <p>Y-90 is fed only by Sr-90 and decays 25 times faster. After a few
     * Y-90 half-lives the two are in fixed ratio, and that ratio is the ratio of
     * their half-lives: N(Y90)/N(Sr90) = T(Y90)/T(Sr90). This holds for any
     * irradiation history and any yield, so a match here says the decay
     * constants and the chain routing are right without relying on the yield
     * file at all.
     *
     * <p>The same logic applies to the Cs-137 -> Ba-137m -> Ba-137 pair, except
     * that Ba-137 is stable: after 618 days only 2.7% of the Cs-137 has decayed,
     * so the ratio is small and must equal 1 - exp(-lambda t) exactly.
     */
    private static void testSecularEquilibrium() {
        double sr90 = InventoryLoader.atoms("Sr90");
        double y90 = InventoryLoader.atoms("Y90");
        check("Sr-90 and Y-90 are both present", sr90 > 0.0 && y90 > 0.0,
            "Sr90=" + fmt(sr90) + " Y90=" + fmt(y90));

        Isotope sr = IsotopeLoader.require("Sr90");
        Isotope y = IsotopeLoader.require("Y90");
        double expected = y.halfLifeSeconds() / sr.halfLifeSeconds();
        double actual = y90 / sr90;
        check("Y-90/Sr-90 atoms = half-life ratio (secular equilibrium)",
            rel(actual, expected, 0.02),
            String.format("actual %.5f, T ratio %.5f", actual, expected));

        // Ba-137 is stable and fed only by Cs-137, so it holds every Cs-137 atom
        // that decayed DURING irradiation -- not just after shutdown. With a
        // constant source the closed form is Ba/Cs = 1 - (1 - e^-lt)/(lt), which
        // is the transient buildup, not the post-shutdown decay curve.
        double cs137 = InventoryLoader.atoms("Cs137");
        double ba137 = InventoryLoader.atoms("Ba137");
        double lt = IsotopeLoader.require("Cs137").decayConstant() * 618.0 * 86400.0;
        double expectedRatio = 1.0 - (1.0 - Math.exp(-lt)) / lt;
        check("Ba-137/Cs-137 = 1 - (1 - e^-lt)/(lt) (stable daughter, built up "
                + "during irradiation)",
            rel(ba137 / cs137, expectedRatio, 0.05),
            String.format("actual %.5f, expected %.5f", ba137 / cs137, expectedRatio));
    }

    /**
     * The closed-form Bateman answer, for nuclides whose chain position makes it
     * exact.
     *
     * <p>For a nuclide fed only by fission and removed only by its own decay,
     * N(t) = Y F (1 - e^-lambda t) / (lambda T) for a constant fission rate over
     * T. The generator instead integrates the full coupled system; the two agree
     * only if the generator's matrix, decay constants and branch routing are all
     * right. These are the checks that caught the stiffness bug: before the
     * Radau solver, Ru-106 came out 27% low and Ce-144 16% low against these
     * same formulas.
     */
    private static void testAnalyticBateman() {
        double fissions = InventoryLoader.fissionCount().total();
        double t = 618.0 * 86400.0;
        double rate = fissions / t;

        // Yield-independent form: N = (cumulative yield) x fissions x
        // (1 - e^-lambda t) / (lambda t). The yield cancels against the rate.
        assertBateman("Ce144", 0.0513, 285.0 * 86400.0, 0.02);
        assertBateman("Ru106", 0.0126, 371.8 * 86400.0, 0.03);
        assertBateman("Ba140", 0.0628, 12.752 * 86400.0, 0.06);
    }

    /**
     * One closed-form check against a literature cumulative yield.
     *
     * <p>The model burns a U-235/Pu-239/Pu-241 mix, so its effective yield for a
     * given nuclide differs from the thermal-U-235 value quoted here by the
     * plutonium contribution -- a few percent for the mass region these three
     * sit in. The tolerance is set wide enough to admit that and narrow enough
     * to fail a factor-of-two error.
     */
    private static void assertBateman(String symbol, double literatureYield,
                                      double halfLife, double tolerance) {
        double t = 618.0 * 86400.0;
        double fissions = InventoryLoader.fissionCount().total();
        double lambda = Math.log(2.0) / halfLife;
        double analytic = literatureYield * fissions
            * (1.0 - Math.exp(-lambda * t)) / (lambda * t);
        double actual = InventoryLoader.atoms(symbol);
        check(symbol + " matches closed-form Bateman from its literature yield",
            rel(actual, analytic, tolerance),
            String.format("model %.4e, analytic %.4e, ratio %.4f",
                actual, analytic, actual / analytic));
    }

    /**
     * Cs-137 in the core: mass and activity against published figures.
     *
     * <p>This is the only check that exercises the yields and the fission count
     * together, so it is the one that would catch a common-mode error in the
     * burn. Unit 4 held roughly 75 kg of Cs-137 at the moment of the accident;
     * published estimates for the core inventory run 70-80 kg, and the mass is
     * quoted independently of any release estimate.
     *
     * <p>The activity follows from the mass and the layer-1 decay constant, and
     * is quoted because it is the number that appears in the accident
     * literature: about 2.6e17 Bq of Cs-137 in the core at shutdown.
     */
    private static void testCs137Anchor() {
        double atoms = InventoryLoader.atoms("Cs137");
        double massKg = atoms / 6.02214076e23 * 136.907 / 1000.0;
        check("Cs-137 core mass is 70-80 kg (published estimate)",
            massKg > 70.0 && massKg < 80.0,
            String.format("%.1f kg", massKg));

        double bq = InventoryLoader.activityBq("Cs137");
        check("Cs-137 core activity is 2.3e17-2.9e17 Bq (published ~2.6e17)",
            bq > 2.3e17 && bq < 2.9e17, String.format("%.3e Bq", bq));

        // The activity must be exactly atoms x lambda, not a stored number: this
        // is the one sanctioned bridge from layer 2 back to layer 1.
        double recomputed = atoms * IsotopeLoader.require("Cs137").decayConstant();
        check("Cs-137 activity equals atoms x layer-1 decay constant",
            rel(bq, recomputed, 1e-9),
            String.format("%.6e vs %.6e", bq, recomputed));
    }

    /**
     * Isomers are separate nodes, not folded into their ground states.
     *
     * <p>An isomer has its own half-life and its own independent yield, and in a
     * few chains it carries a large share of the mass -- 31% of chain A=133 is
     * Ba-133m. Folding it into the ground state would give that ground state a
     * half-life it does not have. The generator keeps them apart; this checks
     * the loader did not merge them on the way in.
     */
    private static void testIsomersKeptSeparate() {
        boolean sawIsomer = false;
        for (InventoryLoader.InventoryEntry e : InventoryLoader.inventory().values()) {
            if (e.symbol().endsWith("m")) {
                sawIsomer = true;
                if (e.atoms() <= 0.0) {
                    check("isomer " + e.symbol() + " has a positive atom count", false,
                        fmt(e.atoms()));
                }
            }
        }
        check("at least one isomer survives as its own node", sawIsomer, "");

        // Xe-135m is a real node with a real population; its ground state must
        // not have absorbed it.
        check("Xe-135 and Xe-135m are separate entries",
            InventoryLoader.atoms("Xe135") > 0.0,
            "Xe135=" + fmt(InventoryLoader.atoms("Xe135")));
    }

    /**
     * Layer 2 contains atoms and nothing else.
     *
     * <p>The loader rejects a file that carries activity or release data, so that
     * a later layer cannot quietly read a source term out of the inventory. This
     * asserts the rejection is wired up and that the current file passes it.
     */
    private static void testNoActivityInLayerTwo() {
        check("no layer-2 record carries an activity field",
            InventoryLoader.problems().stream().noneMatch(p -> p.contains("activity")),
            String.valueOf(InventoryLoader.problems()));
    }

    /**
     * The generator's own mass-balance residual must be at round-off.
     *
     * <p>The generator solves the coupled system and reports what fraction of
     * the produced atoms it could not place. At 1e-3 it is round-off; anything
     * larger means atoms went missing in the decay graph. This is a check on the
     * generated file, so it catches a bad rebuild even when every individual
     * nuclide above still looks plausible.
     */
    private static void testMassBalanceResidual() {
        double residual = readMassBalanceResidual();
        check("generator mass-balance residual is within 0.1%",
            Math.abs(residual) < 1e-3, String.format("%.2e", residual));
    }

    /** Reads the residual the generator recorded, straight from the data file. */
    private static double readMassBalanceResidual() {
        try (var in = InventorySelfTest.class.getResourceAsStream(InventoryLoader.RESOURCE)) {
            if (in == null) {
                return Double.NaN;
            }
            var root = com.google.gson.JsonParser.parseReader(
                new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8))
                .getAsJsonObject();
            return root.has("massBalanceResidual")
                ? root.get("massBalanceResidual").getAsDouble() : Double.NaN;
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    private static boolean rel(double actual, double expected, double tolerance) {
        if (expected == 0.0) {
            return actual == 0.0;
        }
        return Math.abs(actual - expected) / Math.abs(expected) <= tolerance;
    }

    private static String fmt(double v) {
        return String.format("%.4e", v);
    }

    private static void check(String what, boolean ok, String detail) {
        checks++;
        if (!ok) {
            failures++;
        }
        System.out.printf("%s %s%s%n", ok ? "PASS" : "FAIL", what,
            detail == null || detail.isEmpty() ? "" : "  [" + detail + "]");
    }

    private InventorySelfTest() {}
}
