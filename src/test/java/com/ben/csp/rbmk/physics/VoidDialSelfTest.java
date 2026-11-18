package com.ben.csp.rbmk.physics;

/**
 * Makes the void-worth DIAL visible and proves it is load-bearing.
 *
 * <p>The reactor does not derive its whole-core void coefficient — it derives the
 * SHAPE from the lattice table, then sets the MAGNITUDE with a hand-set factor
 * ({@link RbmkXsec#voidWorthScale()}, 0.233). The point of this test is to stop
 * that fact hiding: it measures the SAME 3D core twice, once with the dial on
 * (what ships) and once with the dial off ({@code 1.0} = the raw derived cell), and
 * asserts that the two differ. If someone later regenerates the lattice table so
 * the raw cell is correct, the dial can be removed — and this test will then tell
 * them the dial has become unnecessary (the two numbers converge).
 *
 * <p>It also prints the ratio between the raw and dialed void worth, which is the
 * size of the error the dial is cancelling. If that ratio is ~1, the dial is a
 * no-op and should go.
 */
public final class VoidDialSelfTest {

    private static int checks = 0;
    private static int failures = 0;

    public static void main(String[] args) {
        System.out.println("=== VoidDialSelfTest ===");
        testDialIsLoadBearing();
        testDialValueReported();
        System.out.printf("%nVoidDialSelfTest: %d checks, %d failures%n", checks, failures);
        if (failures > 0) {
            throw new AssertionError(failures + " check(s) failed");
        }
    }

    /** The whole-core void worth (reactivity convention) at a given dial setting. */
    private static double voidWorth(double dial) {
        double k0 = solve(dial, 0.0);
        double k4 = solve(dial, 0.4);
        return (k4 - k0) / (k0 * k4) / 0.4;
    }

    private static double solve(double dial, double voidFraction) {
        RbmkSolver3D s = new RbmkSolver3D();
        s.setOmega(RbmkSolver3D.SAFE_OMEGA);
        s.setCoreState(voidFraction, 640.0, dial);
        s.setRodInsertion(RbmkTransientSolver.NOMINAL_INSERTION);
        s.solve(3000, 1e-11);
        return s.keff();
    }

    private static void testDialIsLoadBearing() {
        double shipped = voidWorth(RbmkXsec.voidWorthScale());
        double raw = voidWorth(1.0);
        double ratio = shipped != 0 ? raw / shipped : Double.NaN;
        System.out.printf("  dial=%.3f (shipped)  dk/dvoid = %+.4f%n",
            RbmkXsec.voidWorthScale(), shipped);
        System.out.printf("  dial=1.000 (raw)      dk/dvoid = %+.4f%n", raw);
        System.out.printf("  raw/shipped ratio = %.2fx  (the error the dial cancels)%n", ratio);

        check("the dial is load-bearing: raw void worth differs from shipped",
            Math.abs(raw - shipped) > 1e-4,
            String.format("shipped=%+.4f raw=%+.4f", shipped, raw));
        check("the raw (undialed) cell OVERSTATES the void worth vs the shipped value",
            raw > shipped,
            String.format("raw=%+.4f > shipped=%+.4f", raw, shipped));
        check("with the dial the whole-core void worth is near INSAG-7 (+0.020)",
            Math.abs(shipped - 0.020) < 0.006,
            String.format("shipped=%+.4f, target +0.0200", shipped));
    }

    private static void testDialValueReported() {
        check("the dial value is exposed for the in-game readout",
            RbmkXsec.voidWorthScale() > 0 && RbmkXsec.voidWorthScale() <= 1.0,
            String.format("voidWorthScale=%.4f", RbmkXsec.voidWorthScale()));
    }

    private static void check(String name, boolean ok, String detail) {
        checks++;
        if (ok) {
            System.out.printf("  [PASS] %s  (%s)%n", name, detail);
        } else {
            failures++;
            System.out.printf("  [FAIL] %s  (%s)%n", name, detail);
        }
    }
}
