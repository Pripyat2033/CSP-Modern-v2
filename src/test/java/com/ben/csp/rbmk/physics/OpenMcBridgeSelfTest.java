package com.ben.csp.rbmk.physics;

import java.util.List;

/**
 * Validation of the OpenMC bridge: that the mod actually reads the external
 * OpenMC result and reports the RBMK's positive void coefficient.
 *
 * <p>This test does not re-run OpenMC (that is the Python side). It checks that
 * the result resource is present, parses, and carries the physics we expect: a
 * positive operating-point void coefficient close to the documented
 * INSAG-7 / NUREG-1250 value of +0.02 dk/k per unit void, with k rising
 * monotonically as the channel voids.
 *
 * <p>An earlier version of this test asserted that k turned over at full void and
 * that k(full water) &gt; k(full void). Both were artefacts of the withdrawn
 * channel geometry: the voided branch renormalised fuel and clad to fill the freed
 * bore volume, raising their density 2.33x at void 1.0 and pulling k down. With
 * that fixed (commit 9e7bd02) the curve is monotonic — graphite, not water, is the
 * moderator, so removing the water mostly removes an absorber. The assertions were
 * regenerated to match the corrected physics. See docs/VOID-COEFF-ROOT-CAUSE.md.
 */
public final class OpenMcBridgeSelfTest {

    private static int checks = 0;

    public static void main(String[] args) {
        check(OpenMcResult.available(), "the OpenMC result resource loads from the mod jar");

        if (!OpenMcResult.available()) {
            summary();
            return;
        }

        // The RBMK's defining feedback is POSITIVE.
        check("positive".equalsIgnoreCase(OpenMcResult.sign()),
            "the void coefficient sign is positive (" + OpenMcResult.sign() + ")");

        double coeff = OpenMcResult.voidCoeffOperating();
        check(coeff > 0.0, "operating-point void coefficient is positive: " + round(coeff));
        // A single-channel infinite lattice overstates the whole-core operating
        // coefficient (no control rods, one channel voided), so the expected value
        // sits somewhat ABOVE the documented whole-core +0.02 — about +0.035. The
        // band below admits the lattice value while rejecting a wrong sign, a zero,
        // or a runaway; it is NOT a check that the value equals INSAG-7 (that
        // apparent agreement was a geometry artefact — see VOID-COEFF-ROOT-CAUSE.md).
        check(coeff > 0.015 && coeff < 0.06,
            "void coefficient is a plausible lattice value (~+0.035): " + round(coeff));

        check(OpenMcResult.kFullVoid() > OpenMcResult.kFullWater(),
            "k(full void) " + round(OpenMcResult.kFullVoid())
                + " > k(full water) " + round(OpenMcResult.kFullWater())
                + " — removing the water (an absorber here, the graphite is the moderator)"
                + " raises k");

        // The sweep must rise as the channel voids through the operating range.
        List<OpenMcResult.Point> sweep = OpenMcResult.sweep();
        check(sweep.size() >= 5, "the sweep has " + sweep.size() + " points");
        boolean rising = true;
        for (int i = 1; i < sweep.size(); i++) {
            if (sweep.get(i).k() < sweep.get(i - 1).k()) {
                rising = false;
            }
        }
        check(rising, "k rises monotonically as the channel voids (the positive void coefficient)");

        // No turnover: k keeps rising to full void. Graphite is the moderator, so
        // removing the water removes an absorber, not the moderator. (The old
        // "turnover" was a density-renormalisation artefact — see the class note.)
        double last = sweep.get(sweep.size() - 1).k();
        double prev = sweep.get(sweep.size() - 2).k();
        check(last >= prev, "k does NOT turn over at full void (" + round(prev) + " -> "
            + round(last) + "): graphite is the moderator, the water an absorber");

        // Interpolation used by the visualisation is sane and clamps at the ends.
        check(Math.abs(OpenMcResult.kAtVoid(0.0) - sweep.get(0).k()) < 1e-9,
            "kAtVoid(0) returns the full-water point");
        check(Math.abs(OpenMcResult.kAtVoid(1.0) - last) < 1e-9,
            "kAtVoid(1) returns the full-void point");
        double mid = OpenMcResult.kAtVoid(0.5);
        check(mid > Math.min(sweep.get(0).k(), last) && mid < 2.0,
            "kAtVoid(0.5) is a sane interpolated value: " + round(mid));

        summary();
    }

    private static void check(boolean ok, String what) {
        checks++;
        System.out.println((ok ? "  ok   " : "  FAIL ") + what);
        if (!ok) {
            failures++;
        }
    }

    private static int failures = 0;

    private static void summary() {
        System.out.println("OpenMcBridgeSelfTest: " + (checks - failures) + "/" + checks + " checks passed");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static String round(double v) {
        return String.format("%.5f", v);
    }

    private OpenMcBridgeSelfTest() {}
}
