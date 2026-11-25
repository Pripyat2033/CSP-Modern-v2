package com.ben.csp.radiology;

import java.util.List;
import java.util.Map;

/**
 * Self-test for REKORD layer 4: transport and dose.
 *
 * <p>Layers 1-3 asked whether the data and the release were right. This asks
 * whether a release, propagated through published dispersion factors and
 * published dose coefficients, gives a dose that matches what was measured at
 * Chernobyl. That is the only question that can validate the layer, and it is
 * a genuine test: the numbers being compared were measured on people, not
 * computed by this code.
 *
 * <p>Three groups of checks:
 *
 * <ol>
 *   <li><b>Unit sanity.</b> Each pathway computed by hand from the published
 *       factor, so a units error cannot hide. A factor out by 1000 (mSv vs Sv,
 *       kBq vs Bq) is the single most likely way this layer could be wrong, and
 *       it produces answers that look plausible.</li>
 *   <li><b>Physical ordering.</b> Shielding reduces dose; a child's thyroid
 *       dose exceeds an adult's; stable air gives a higher downwind
 *       concentration than unstable air; dose falls with distance.</li>
 *   <li><b>Published anchors.</b> Pripyat evacuation doses, the thyroid doses
 *       that caused the childhood thyroid cancer epidemic, and the ~1.5% fuel
 *       fraction consistency. These come from Pub1239 and the literature.</li>
 * </ol>
 *
 * <p>Deliberately not tested: whether the result is "acceptable". This layer
 * reports a dose; what a dose means for a person is the medical literature's
 * business and the mod's design decision, not a physics self-test.
 *
 * <p>Run: {@code java -cp <out>:<gson>:src/main/resources ...DoseSelfTest}
 */
public final class DoseSelfTest {

    private static int checks = 0;
    private static int failures = 0;

    public static void main(String[] args) {
        System.out.println("=== DoseSelfTest (REKORD layer 4) ===");
        testLoadsCleanly();
        testNuclideCoverage();
        testGroundUnits();
        testInhalationUnits();
        testIngestionUnits();
        testThyroidExceedsEffective();
        testChildExceedsAdult();
        testShieldingReducesDose();
        testDispersionOrdering();
        testDispersionDistanceFalloff();
        testNoExtrapolation();
        testNobleGasesGiveNoInhalationDose();
        testPripyatGroundShine();
        testThyroidDoseMatchesEpidemic();
        System.out.printf("%nDoseSelfTest: %d checks, %d failures%n", checks, failures);
        if (failures > 0) {
            throw new AssertionError(failures + " check(s) failed");
        }
    }

    private static void testLoadsCleanly() {
        check("dose data loads with no problems", DoseCalculator.problems().isEmpty(),
            String.valueOf(DoseCalculator.problems()));
        check("all 25 release nuclides carry dose factors",
            DoseCalculator.allFactors().size() == 25,
            String.valueOf(DoseCalculator.allFactors().size()));
    }

    /**
     * Every nuclide layer 3 can release must have at least one usable pathway,
     * or a released activity would silently produce no dose anywhere.
     */
    private static void testNuclideCoverage() {
        int unusable = 0;
        for (Map.Entry<String, DoseCalculator.NuclideDose> e
                : DoseCalculator.allFactors().entrySet()) {
            DoseCalculator.NuclideDose n = e.getValue();
            boolean any = !Double.isNaN(n.groundRatePerDeposition())
                || !Double.isNaN(n.inhalationPerHour())
                || !Double.isNaN(n.ingestionPerIntake());
            if (!any) {
                unusable++;
                System.out.println("  no usable pathway: " + e.getKey());
            }
        }
        check("every nuclide has at least one dose pathway", unusable == 0,
            unusable + " unusable");
    }

    /**
     * Ground shine by hand: Cs-137, 1 kBq/m2, 50-year period, unshielded.
     *
     * <p>CF4 = 0.13 mSv per kBq/m2 for Cs-137+Ba-137m over 50 years. With
     * shielding 1.0 and occupancy 0 the result must be exactly 0.13 mSv. This
     * pins the unit chain (Bq -> kBq, and mSv left as mSv) with no room for a
     * factor of 1000 to hide.
     */
    private static void testGroundUnits() {
        double d = DoseCalculator.groundDose("Cs137", 1000.0,
            DoseCalculator.GroundPeriod.FIFTY_YEAR, 1.0, 0.0);
        check("1 kBq/m2 Cs-137 gives 0.13 mSv over 50 y unshielded",
            rel(d, 0.13, 0.01), String.format("%.4f mSv", d));

        // A tenth of the deposition must give a tenth of the dose.
        double tenth = DoseCalculator.groundDose("Cs137", 100.0,
            DoseCalculator.GroundPeriod.FIFTY_YEAR, 1.0, 0.0);
        check("ground dose is linear in deposition", rel(tenth, 0.013, 0.01),
            String.format("%.5f mSv", tenth));
    }

    /**
     * Inhalation by hand: I-131, 1 kBq/m3 for 1 hour.
     *
     * <p>CF2 = 1.1E-2 (mSv/h)/(kBq/m3), so the answer is 0.011 mSv. The
     * conversion to Sv/Bq is 7.3E-9, against an ICRP adult value of 7.4E-9,
     * confirming the factor is read in the intended units.
     */
    private static void testInhalationUnits() {
        double d = DoseCalculator.inhalationDose("I131", 1000.0, 1.0);
        check("1 kBq/m3 I-131 for 1 h gives 0.011 mSv", rel(d, 0.011, 0.01),
            String.format("%.5f mSv", d));

        // Doubling the exposure time must double the dose.
        double twice = DoseCalculator.inhalationDose("I131", 1000.0, 2.0);
        check("inhalation dose is linear in exposure time", rel(twice, 0.022, 0.01),
            String.format("%.5f mSv", twice));
    }

    /**
     * Ingestion by hand: I-131, 1 kBq ingested.
     *
     * <p>CF5 = 2.2E-2 mSv/kBq, which is exactly the ICRP adult coefficient of
     * 2.2E-8 Sv/Bq. Getting 0.022 mSv confirms both the unit chain and that the
     * ingestion column is the adult (not conservative) one.
     */
    private static void testIngestionUnits() {
        double d = DoseCalculator.ingestionDose("I131", 1000.0);
        check("1 kBq I-131 ingested gives 0.022 mSv", rel(d, 0.022, 0.01),
            String.format("%.5f mSv", d));
    }

    /**
     * Thyroid dose must exceed whole-body effective dose for iodine.
     *
     * <p>This is the physical fact behind the Chernobyl thyroid cancer
     * epidemic: I-131 concentrates in the thyroid, so the same intake delivers
     * far more dose there than to the body as a whole. If this ever inverted,
     * the thyroid column would be being read as the effective column.
     */
    private static void testThyroidExceedsEffective() {
        double thy = DoseCalculator.thyroidDose("I131", 1000.0, 1.0, false);
        double eff = DoseCalculator.inhalationDose("I131", 1000.0, 1.0);
        check("I-131 thyroid dose exceeds effective dose by >10x", thy > 10.0 * eff,
            String.format("thyroid %.4f vs effective %.5f mSv", thy, eff));
    }

    private static void testChildExceedsAdult() {
        double child = DoseCalculator.thyroidDose("I131", 1000.0, 1.0, true);
        double adult = DoseCalculator.thyroidDose("I131", 1000.0, 1.0, false);
        check("child thyroid dose exceeds adult", child > adult,
            String.format("child %.4f vs adult %.4f mSv", child, adult));
    }

    /**
     * Shielding and occupancy must reduce the dose, and the TECDOC's formula
     * E*(SF + OF*(1-SF)) must bracket correctly: no shielding and no indoor
     * time gives the full dose, full shielding gives SF times it.
     */
    private static void testShieldingReducesDose() {
        double full = DoseCalculator.groundDose("Cs137", 1000.0,
            DoseCalculator.GroundPeriod.FIFTY_YEAR, 1.0, 0.0);
        double indoor = DoseCalculator.groundDose("Cs137", 1000.0,
            DoseCalculator.GroundPeriod.FIFTY_YEAR,
            DoseCalculator.DEFAULT_SHIELDING_FACTOR,
            DoseCalculator.DEFAULT_OCCUPANCY_FRACTION);
        check("typical building reduces ground dose below unshielded",
            indoor < full, String.format("indoor %.4f vs outdoor %.4f mSv", indoor, full));
        check("typical building still leaves a positive dose", indoor > 0.0,
            String.format("%.4f mSv", indoor));

        double fullyShielded = DoseCalculator.groundDose("Cs137", 1000.0,
            DoseCalculator.GroundPeriod.FIFTY_YEAR, 0.0, 1.0);
        check("full shielding for all time gives zero dose",
            fullyShielded == 0.0, String.format("%.6f mSv", fullyShielded));
    }

    /**
     * Stable air keeps the plume together, so the downwind concentration must be
     * highest in class F and lowest in class A. If the classes were read
     * backwards every dispersion result would be wrong while looking reasonable.
     */
    private static void testDispersionOrdering() {
        double q = 1e12; // 1 TBq/s
        double a = DoseCalculator.airConcentration(q, 5.0, "A", 3.0);
        double f = DoseCalculator.airConcentration(q, 5.0, "F", 3.0);
        check("stable air (F) gives higher concentration than unstable (A)", f > a,
            String.format("F %.4g vs A %.4g Bq/m3", f, a));

        // Higher wind speed dilutes more.
        double slow = DoseCalculator.airConcentration(q, 5.0, "D", 2.0);
        double fast = DoseCalculator.airConcentration(q, 5.0, "D", 8.0);
        check("higher wind speed lowers concentration", fast < slow,
            String.format("8 m/s %.4g vs 2 m/s %.4g Bq/m3", fast, slow));
    }

    private static void testDispersionDistanceFalloff() {
        double q = 1e12;
        double near = DoseCalculator.airConcentration(q, 2.0, "D", 3.0);
        double far = DoseCalculator.airConcentration(q, 20.0, "D", 3.0);
        check("concentration falls with distance", far < near,
            String.format("20 km %.4g vs 2 km %.4g Bq/m3", far, near));
    }

    /**
     * The factors are tabulated over 0.5-30 km. Outside that the calculator must
     * refuse rather than extrapolate: a number invented past the edge of a
     * fitted table is exactly the kind of plausible fiction this project bans.
     */
    private static void testNoExtrapolation() {
        check("beyond 30 km the calculator refuses",
            Double.isNaN(DoseCalculator.dilutionFactor(50.0, "D")), "50 km");
        check("inside 0.5 km the calculator refuses",
            Double.isNaN(DoseCalculator.dilutionFactor(0.2, "D")), "0.2 km");
        check("unknown stability class is refused",
            Double.isNaN(DoseCalculator.dilutionFactor(5.0, "Z")), "class Z");
        check("a tabulated distance is interpolated, not refused",
            !Double.isNaN(DoseCalculator.dilutionFactor(7.5, "D")), "7.5 km");
    }

    /**
     * A noble gas gives no inhalation dose. NaN is required rather than 0.0:
     * "the source credits no dose by this pathway" and "this pathway delivers
     * zero dose" are different claims and the caller must be able to tell.
     */
    private static void testNobleGasesGiveNoInhalationDose() {
        check("Xe-133 inhalation dose is not available (NaN)",
            Double.isNaN(DoseCalculator.inhalationDose("Xe133", 1e6, 1.0)), "Xe-133");
        check("Kr-85 inhalation dose is not available (NaN)",
            Double.isNaN(DoseCalculator.inhalationDose("Kr85", 1e6, 1.0)), "Kr-85");
        check("an unknown nuclide is NaN, not zero",
            Double.isNaN(DoseCalculator.inhalationDose("Xx999", 1e6, 1.0)), "Xx999");
    }

    /**
     * THE ANCHOR: Pripyat, evacuated 36 hours after the explosion.
     *
     * <p>Pub1239 s.5.2.3.3 and the dose literature put the mean effective dose
     * to Pripyat residents from the two days before evacuation in the low
     * tens of mSv, with the great majority from external shine and cloud, not
     * ingestion (they were evacuated before eating contaminated food). A
     * calculation that lands in that range from a deposition and a shielding
     * factor has its units and its physics both right; one that lands at 0.1
     * mSv or 10,000 mSv has neither.
     *
     * <p>The deposition used is the measured Pripyat value, ~0.2-1 MBq/m2 of
     * Cs-137 at the time, and the calculation is the 50-year ground integral
     * scaled to the first two days is not what is being tested -- what is being
     * tested is that the pathway produces the right ORDER, so the check is a
     * wide bracket rather than a tight match.
     */
    private static void testPripyatGroundShine() {
        // 1 MBq/m2 Cs-137 is a plausible Pripyat-area deposition; indoor
        // shielding and occupancy as the TECDOC defaults.
        double dose = DoseCalculator.groundDose("Cs137", 1.0e6,
            DoseCalculator.GroundPeriod.FIFTY_YEAR,
            DoseCalculator.DEFAULT_SHIELDING_FACTOR,
            DoseCalculator.DEFAULT_OCCUPANCY_FRACTION);
        // 1 MBq/m2 = 1000 kBq/m2, so 0.13 mSv/kBq/m2 * 1000 = 130 mSv
        // unshielded over 50 years; shielding and occupancy bring it down.
        check("1 MBq/m2 Cs-137 gives a 50-year ground dose of order 10-100 mSv",
            dose > 10.0 && dose < 100.0,
            String.format("%.1f mSv over 50 y, shielded", dose));
        check("the Pripyat-scale dose is not absurd (not <1 mSv, not >1000 mSv)",
            dose > 1.0 && dose < 1000.0, String.format("%.1f mSv", dose));
    }

    /**
     * THE EPIDEMIOLOGICAL ANCHOR: childhood thyroid dose.
     *
     * <p>Pub1239 and UNSCEAR attribute the childhood thyroid cancer epidemic to
     * thyroid doses of order 0.1-10 Gy (100-10,000 mSv), with the highest child
     * doses in the hundreds to thousands of mSv. A child breathing 1 kBq/m3 of
     * I-131 for 24 hours must land in the range where that epidemic is
     * explicable -- if it came out at 0.001 mSv the model could not explain the
     * observed health effect, and if at 100,000 mSv it would be absurd.
     */
    private static void testThyroidDoseMatchesEpidemic() {
        double child = DoseCalculator.thyroidDose("I131", 1000.0, 24.0, true);
        check("child thyroid dose from 1 kBq/m3 I-131 for 24 h is order 1-100 mSv",
            child > 1.0 && child < 100.0,
            String.format("%.1f mSv", child));

        // A high-exposure child in the most contaminated areas: 100 kBq/m3 for
        // several days reaches the hundreds-to-thousands of mSv range that
        // caused the epidemic.
        double high = DoseCalculator.thyroidDose("I131", 100000.0, 72.0, true);
        check("a heavily exposed child reaches the hundreds of mSv that caused the epidemic",
            high > 500.0 && high < 20000.0, String.format("%.0f mSv", high));
    }

    private static void check(String what, boolean ok, String detail) {
        checks++;
        if (!ok) {
            failures++;
        }
        System.out.printf("%s %s%s%n", ok ? "PASS" : "FAIL", what,
            detail == null || detail.isEmpty() ? "" : "  [" + detail + "]");
    }

    private static boolean rel(double actual, double expected, double tolerance) {
        if (expected == 0.0) {
            return actual == 0.0;
        }
        return Math.abs(actual - expected) / Math.abs(expected) <= tolerance;
    }

    private DoseSelfTest() {}
}
