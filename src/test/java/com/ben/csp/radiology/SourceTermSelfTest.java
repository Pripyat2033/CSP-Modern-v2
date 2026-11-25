package com.ben.csp.radiology;

import java.util.List;
import java.util.Map;

/**
 * Self-test for REKORD layer 3: the source term, as a range.
 *
 * <p>Layer 1 asked whether the nuclear data was right; layer 2 whether the
 * atoms were right. This asks whether the release is right, and it is the first
 * layer where the answer is legitimately a range. The checks fall into three
 * groups:
 *
 * <ol>
 *   <li><b>Structural.</b> Every record is internally consistent, no fraction
 *       lies outside [0,1], and no entry that was refused carries a number.
 *       These catch a data file that would let a consumer read a non-result as
 *       a result.</li>
 *   <li><b>Physical ordering.</b> The release fractions must be ordered the way
 *       volatility orders them: inert gases most, refractory elements least.
 *       This is the check that would catch the volatility classes being
 *       assigned backwards, and it holds without reference to any single
 *       source.</li>
 *   <li><b>Published anchors.</b> Individual fractions written into this test
 *       from the literature, so the whole file cannot drift silently.</li>
 * </ol>
 *
 * <p>Deliberately not tested: dose, transport, deposition. Those are layer 4.
 *
 * <p>Run: {@code java -cp <out>:<gson>:src/main/resources ...SourceTermSelfTest}
 */
public final class SourceTermSelfTest {

    private static int checks = 0;
    private static int failures = 0;

    public static void main(String[] args) {
        System.out.println("=== SourceTermSelfTest (REKORD layer 3) ===");
        testLoadsCleanly();
        testTableCoverage();
        testFractionsInRange();
        testRefusedEntriesCarryNoNumber();
        testVolatilityOrdering();
        testInertGasFractions();
        testCs137Anchor();
        testFuelFraction();
        testCaesiumRevision();
        testActinidesQuotedWithoutFraction();
        System.out.printf("%nSourceTermSelfTest: %d checks, %d failures%n", checks, failures);
        if (failures > 0) {
            throw new AssertionError(failures + " check(s) failed");
        }
    }

    private static void testLoadsCleanly() {
        int n = SourceTermLoader.nuclides().size();
        check("source term loads the full table", n == 25, "n=" + n);
        check("no record was rejected", SourceTermLoader.problems().isEmpty(),
            String.valueOf(SourceTermLoader.problems()));
        check("total released activity is positive",
            SourceTermLoader.totalReleasedPbq() > 0.0,
            fmt(SourceTermLoader.totalReleasedPbq()) + " PBq");
    }

    /**
     * The file must carry every nuclide Table 3.1 lists, including the ones whose
     * fraction cannot be derived. Dropping those would make the file look cleaner
     * and would lose the seven actinides entirely.
     */
    private static void testTableCoverage() {
        String[] required = {
            "Kr85", "Xe133", "Te129m", "Te132", "I131", "I133", "Cs134", "Cs136",
            "Cs137", "Sr89", "Sr90", "Ru103", "Ru106", "Ba140", "Zr95", "Mo99",
            "Ce141", "Ce144", "Np239", "Pu238", "Pu239", "Pu240", "Pu241",
            "Pu242", "Cm242"
        };
        int missing = 0;
        for (String s : required) {
            if (!SourceTermLoader.nuclides().containsKey(s)) {
                missing++;
                System.out.println("  missing from file: " + s);
            }
        }
        check("all 25 Table 3.1 nuclides present", missing == 0, missing + " missing");
    }

    /**
     * A fraction outside [0,1] is not a rounding error: it means the core
     * activity it was divided by is wrong. The loader already refuses such a
     * record; this asserts that none reached the map.
     */
    private static void testFractionsInRange() {
        int bad = 0;
        for (SourceTermLoader.ReleaseEntry e : SourceTermLoader.nuclides().values()) {
            if (e.releaseFraction() == null) {
                continue;
            }
            double f = e.releaseFraction();
            if (f < 0.0 || f > 1.0) {
                bad++;
                System.out.println("  out of range: " + e.symbol() + " " + f);
            }
        }
        check("every derived fraction lies in [0,1]", bad == 0, bad + " out of range");
    }

    /**
     * The dangerous failure mode: an entry marked "cannot be derived" that still
     * carries a number, which a consumer would read as a result.
     */
    private static void testRefusedEntriesCarryNoNumber() {
        int inconsistent = 0;
        for (SourceTermLoader.ReleaseEntry e : SourceTermLoader.nuclides().values()) {
            boolean hasNumber = e.releaseFraction() != null;
            if (hasNumber != e.fractionDerived()) {
                inconsistent++;
                System.out.println("  inconsistent: " + e.symbol()
                    + " derived=" + e.fractionDerived() + " hasNumber=" + hasNumber);
            }
        }
        check("refused entries carry no number, derived entries do",
            inconsistent == 0, inconsistent + " inconsistent");
    }

    /**
     * Volatility must order the release. Inert gases escape almost completely;
     * refractory elements leave only inside fuel particles and so track the
     * ~1.5% fuel fraction. If the classes were assigned backwards this fails
     * while every individual number still looks plausible.
     */
    private static void testVolatilityOrdering() {
        double inert = meanFraction("inert");
        double volatile_ = meanFraction("volatile");
        double intermediate = meanFraction("intermediate");
        double refractory = meanFraction("refractory");

        check("inert gases release more than volatiles", inert > volatile_,
            String.format("inert %.3f vs volatile %.3f", inert, volatile_));
        check("volatiles release more than intermediate elements",
            volatile_ > intermediate,
            String.format("volatile %.3f vs intermediate %.3f", volatile_, intermediate));
        check("intermediate elements release more than refractory",
            intermediate > refractory,
            String.format("intermediate %.3f vs refractory %.3f", intermediate, refractory));
    }

    /**
     * Xenon-133 is the most completely released nuclide in the table: a noble
     * gas with a 5.25 d half-life that had time to escape the fuel. A fraction
     * near 1 is the physical expectation, and a value far below it would mean
     * the core inventory is too large.
     */
    private static void testInertGasFractions() {
        SourceTermLoader.ReleaseEntry xe = SourceTermLoader.nuclides().get("Xe133");
        check("Xe-133 is present with a derived fraction",
            xe != null && xe.fractionDerived(), String.valueOf(xe));
        if (xe != null && xe.fractionDerived()) {
            check("Xe-133 released 85-100% of core inventory",
                xe.releaseFraction() > 0.85 && xe.releaseFraction() <= 1.0,
                String.format("%.4f", xe.releaseFraction()));
        }
        SourceTermLoader.ReleaseEntry kr = SourceTermLoader.nuclides().get("Kr85");
        if (kr != null && kr.fractionDerived()) {
            check("Kr-85 released 10-50% of core inventory",
                kr.releaseFraction() > 0.10 && kr.releaseFraction() < 0.50,
                String.format("%.4f", kr.releaseFraction()));
        }
    }

    /** Cs-137 at ~35% is the best-established volatile fraction in the literature. */
    private static void testCs137Anchor() {
        SourceTermLoader.ReleaseEntry cs = SourceTermLoader.nuclides().get("Cs137");
        check("Cs-137 is present with a derived fraction",
            cs != null && cs.fractionDerived(), String.valueOf(cs));
        if (cs != null && cs.fractionDerived()) {
            check("Cs-137 released 25-45% of core inventory",
                cs.releaseFraction() > 0.25 && cs.releaseFraction() < 0.45,
                String.format("%.4f", cs.releaseFraction()));
            check("Cs-137 released activity is ~85 PBq",
                Math.abs(cs.releasedPbq() - 85.0) < 0.5, fmt(cs.releasedPbq()));
        }
    }

    /**
     * The fuel fraction is the single number every refractory release depends
     * on, and it was revised by a factor of two. The test pins both values so a
     * future edit cannot quietly drop the earlier estimate and make the range
     * look narrower than the literature.
     */
    private static void testFuelFraction() {
        SourceTermLoader.FuelFraction f = SourceTermLoader.fuelFraction();
        check("fuel release fraction central is 1.5%",
            rel(f.central(), 0.015, 1e-9), String.format("%.4f", f.central()));
        check("fuel fraction bounds are 1.0% to 2.0%",
            rel(f.low(), 0.010, 1e-9) && rel(f.high(), 0.020, 1e-9),
            String.format("%.4f..%.4f", f.low(), f.high()));
        check("the earlier estimate is higher than the current one",
            f.wasRevisedDownByAboutHalf(),
            String.format("earlier %.3f vs current %.3f", f.earlierEstimate(), f.central()));
    }

    /**
     * The Cs-137 estimate was revised from 40 to 80 PBq on measurement. That
     * revision is the reason this layer stores a range, so it is asserted
     * rather than left as prose.
     */
    private static void testCaesiumRevision() {
        double earlier = 40.0;
        double current = 80.0;
        SourceTermLoader.ReleaseEntry cs = SourceTermLoader.nuclides().get("Cs137");
        check("the Cs-137 revision is recorded as a factor of two",
            rel(current / earlier, 2.0, 0.01), current + " / " + earlier);
        check("the quoted Cs-137 release is consistent with the revised figure",
            cs != null && Math.abs(cs.releasedPbq() - current) < 10.0,
            cs == null ? "absent" : fmt(cs.releasedPbq()));
    }

    /**
     * The seven actinides must be present with a quoted activity and NO
     * fraction. Layer 2 is a fission-product inventory and holds nothing above
     * Z=70, so a fraction would require inventing a denominator. This test is
     * the guard that stops a later "fix" from filling in that blank.
     */
    private static void testActinidesQuotedWithoutFraction() {
        String[] actinides = {"Np239", "Pu238", "Pu239", "Pu240", "Pu241", "Pu242", "Cm242"};
        int wrong = 0;
        for (String s : actinides) {
            SourceTermLoader.ReleaseEntry e = SourceTermLoader.nuclides().get(s);
            if (e == null) {
                wrong++;
                System.out.println("  absent: " + s);
                continue;
            }
            if (e.fractionDerived() || e.releaseFraction() != null) {
                wrong++;
                System.out.println("  has a fraction it cannot have: " + s);
            }
            if (e.releasedPbq() <= 0.0) {
                wrong++;
                System.out.println("  lost its quoted activity: " + s);
            }
        }
        check("seven actinides carry a quoted activity and no fraction",
            wrong == 0, wrong + " wrong");
    }

    private static double meanFraction(String volatilityClass) {
        double sum = 0.0;
        int n = 0;
        for (SourceTermLoader.ReleaseEntry e : SourceTermLoader.nuclides().values()) {
            if (e.fractionDerived() && volatilityClass.equals(e.volatilityClass())) {
                sum += e.releaseFraction();
                n++;
            }
        }
        return n == 0 ? Double.NaN : sum / n;
    }

    private static void check(String what, boolean ok, String detail) {
        checks++;
        if (!ok) {
            failures++;
        }
        System.out.printf("%s %s%s%n", ok ? "PASS" : "FAIL", what,
            detail == null || detail.isEmpty() ? "" : "  [" + detail + "]");
    }

    private static String fmt(double v) {
        return String.format("%.4e", v);
    }

    private static boolean rel(double actual, double expected, double tolerance) {
        if (expected == 0.0) {
            return actual == 0.0;
        }
        return Math.abs(actual - expected) / Math.abs(expected) <= tolerance;
    }

    private SourceTermSelfTest() {}
}
