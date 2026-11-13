package com.ben.csp.rbmk.physics;

import java.util.List;

/**
 * Validation of the OpenMC bridge: that the mod actually reads the external
 * OpenMC result and reports the RBMK's positive void coefficient.
 *
 * <p>This test does not re-run OpenMC (that is the Python side). It checks that
 * the result resource is present, parses, and carries the physics we expect: a
 * positive operating-point void coefficient close to the documented
 * INSAG-7 / NUREG-1250 value of +0.02 dk/k per unit void, with the sweep rising
 * as the channel voids and turning over only at full void.
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
        // Same order as the documented +0.02/unit; the homogenised cell is a
        // coarse approximation, so a factor-of-two band is the honest check.
        check(coeff > 0.008 && coeff < 0.03,
            "void coefficient is the right order (documented ~+0.02): " + round(coeff));

        check(OpenMcResult.kFullWater() > OpenMcResult.kFullVoid(),
            "k(full water) " + round(OpenMcResult.kFullWater())
                + " > k(full void) " + round(OpenMcResult.kFullVoid())
                + " — a fresh cold lattice is supercritical at both ends");

        // The sweep must rise as the channel voids through the operating range.
        List<OpenMcResult.Point> sweep = OpenMcResult.sweep();
        check(sweep.size() >= 5, "the sweep has " + sweep.size() + " points");
        boolean rising = true;
        for (int i = 1; i < sweep.size() - 1; i++) {
            if (sweep.get(i).k() < sweep.get(i - 1).k()) {
                rising = false;
            }
        }
        check(rising, "k rises monotonically as the channel voids (the positive void coefficient)");

        // Turnover only at full void: the last point is lower than the one before.
        double last = sweep.get(sweep.size() - 1).k();
        double prev = sweep.get(sweep.size() - 2).k();
        check(last < prev, "k turns over at full void (" + round(prev) + " -> " + round(last)
            + ") because the water is also a moderator");

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
