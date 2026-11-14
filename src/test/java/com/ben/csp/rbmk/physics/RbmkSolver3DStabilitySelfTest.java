package com.ben.csp.rbmk.physics;

/**
 * Self-test for {@link RbmkSolver3D}'s SOR stability. This pins the bug that the
 * 3D solver's over-relaxation factor was set past its divergence threshold:
 * at omega = 1.85 the flux went negative on ~28,000 nodes and k_eff returned a
 * meaningless negative value, which then poisoned {@code RbmkTransientSolver}
 * (it scaled the fission source by 1/k). The physics was always right; the
 * relaxation factor was not.
 *
 * <ul>
 *   <li>the safe factor converges to a positive k_eff with an all-positive flux;</li>
 *   <li>the old 1.85 value is rejected (clamped), so it can never be set again;</li>
 *   <li>across the void / temperature / rod envelope the solve stays physical.</li>
 * </ul>
 */
public final class RbmkSolver3DStabilitySelfTest {

    private static int checks = 0;

    private static void check(boolean ok, String what) {
        checks++;
        if (!ok) {
            throw new AssertionError("FAILED: " + what);
        }
    }

    private static boolean fluxAllPositive(RbmkSolver3D s) {
        for (int i = 0; i < s.phi1.length; i++) {
            if (s.phi1[i] < 0 || s.phi2[i] < 0) {
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) {
        // --- 1. the safe factor converges, positively ---
        RbmkSolver3D s = new RbmkSolver3D();
        s.setOmega(RbmkSolver3D.SAFE_OMEGA);
        int iters = s.solve(600, 1e-7);
        check(iters < 600, "safe omega converges within 600 outer iterations");
        check(s.keff() > 0 && Double.isFinite(s.keff()),
            "safe omega gives a positive, finite k_eff");
        check(fluxAllPositive(s), "safe omega leaves an all-positive flux");
        check(!s.diverged(), "safe omega does not flag divergence");

        // --- 2. the old 1.85 value is clamped away and cannot diverge the solve ---
        RbmkSolver3D clamped = new RbmkSolver3D();
        clamped.setOmega(1.85);
        clamped.solve(600, 1e-7);
        check(!clamped.diverged(), "setOmega(1.85) is clamped to the safe ceiling");
        check(clamped.keff() > 0, "clamped solve still gives a positive k_eff");

        // --- 3. physical across the whole void / temperature / rod envelope ---
        double[] voids = {0.0, 0.5, 1.0};
        double[] temps = {300.0, 600.0, 900.0};
        double[] rods = {0.0, 0.5, 1.0};
        for (double v : voids) {
            for (double t : temps) {
                for (double r : rods) {
                    RbmkSolver3D c = new RbmkSolver3D();
                    c.setOmega(RbmkSolver3D.SAFE_OMEGA);
                    c.setInnerSweeps(3);
                    c.setCoreState(v, t);
                    c.setRodInsertion(r);
                    c.solve(600, 1e-7);
                    check(!c.diverged() && c.keff() > 0 && fluxAllPositive(c),
                        "physical solve at void=" + v + " T=" + t + " rod=" + r);
                }
            }
        }

        // --- 4. the sign of the feedbacks is right (a reactor, not a guess) ---
        double kCold = keffAt(0.0, 300.0, 0.5);
        double kVoid = keffAt(1.0, 300.0, 0.5);
        double kHot = keffAt(0.0, 900.0, 0.5);
        check(kVoid > kCold, "voiding raises k_eff (positive void coefficient)");
        check(kHot < kCold, "hotter fuel lowers k_eff (negative Doppler)");

        System.out.println("RbmkSolver3DStabilitySelfTest: " + checks + " checks PASS");
    }

    private static double keffAt(double voidFraction, double fuelTempC, double rod) {
        RbmkSolver3D s = new RbmkSolver3D();
        s.setOmega(RbmkSolver3D.SAFE_OMEGA);
        s.setInnerSweeps(3);
        s.setCoreState(voidFraction, fuelTempC);
        s.setRodInsertion(rod);
        s.solve(600, 1e-7);
        return s.keff();
    }
}
