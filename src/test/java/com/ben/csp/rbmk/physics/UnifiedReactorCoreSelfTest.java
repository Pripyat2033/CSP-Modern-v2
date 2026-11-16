package com.ben.csp.rbmk.physics;

/**
 * Self-test for {@link UnifiedReactorCore} — the single core the project runs on.
 *
 * <p>It checks the behaviours a real RBMK-1000 must show, and that the three
 * earlier cores got wrong or could not show at all:
 * <ol>
 *   <li>it holds a steady state at nominal power (the operator can run it);</li>
 *   <li>withdrawing the rods RAISES the power (positive period);</li>
 *   <li>AZ-5 shuts it DOWN;</li>
 *   <li>the VOID coefficient is POSITIVE (the fatal RBMK property) — and it is
 *       DERIVED from the cross sections, not fitted;</li>
 *   <li>the SCRAM is POSITIVE at low insertion (the graphite displacer) — also
 *       derived from the rod geometry.</li>
 * </ol>
 *
 * <p>Checks 4 and 5 are the point of the exercise. They are the mechanisms the
 * lumped core could only ASSERT (via a hand-fitted curve) and the open-loop core
 * could not show at all. Here they emerge from the physics, so the core can be
 * wrong in the right way.
 *
 * <p>Run: {@code java -cp <out>:<gson>:src/main/resources ...UnifiedReactorCoreSelfTest}
 */
public final class UnifiedReactorCoreSelfTest {

    private static int checks = 0;
    private static int failures = 0;

    public static void main(String[] args) {
        System.out.println("=== UnifiedReactorCoreSelfTest ===");
        testHoldsNominal();
        testRodWithdrawalRaises();
        testAz5ShutsDown();
        testVoidCoefficientPositive();
        testPositiveScram();
        testDecayHeatFloor();
        System.out.printf("%nUnifiedReactorCoreSelfTest: %d checks, %d failures%n",
            checks, failures);
        if (failures > 0) {
            throw new AssertionError(failures + " check(s) failed");
        }
    }

    /** 1. A steady, critical core at nominal power. */
    private static void testHoldsNominal() {
        UnifiedReactorCore c = new UnifiedReactorCore();
        for (int k = 0; k < 6000; k++) {
            c.step(0.005);   // 30 s
        }
        double drift = Math.abs(c.fissionPower() - 1.0) * 100;
        check("holds nominal power (30 s drift < 3 %)",
            drift < 3.0, String.format("fission=%.4f (%.1f %% off)", c.fissionPower(), drift));
    }

    /** 2. Withdrawing the rods raises the power (positive period). */
    private static void testRodWithdrawalRaises() {
        UnifiedReactorCore c = new UnifiedReactorCore();
        for (int k = 0; k < 2000; k++) {
            c.step(0.005);
        }
        double before = c.fissionPower();
        c.setRodTarget(0.70);
        for (int k = 0; k < 4000; k++) {
            c.step(0.005);
        }
        check("rod withdrawal (0.80->0.70) RAISES power",
            c.fissionPower() > before * 1.05,
            String.format("%.4f -> %.4f", before, c.fissionPower()));
    }

    /** 3. AZ-5 shuts the reactor down. */
    private static void testAz5ShutsDown() {
        UnifiedReactorCore c = new UnifiedReactorCore();
        for (int k = 0; k < 2000; k++) {
            c.step(0.005);
        }
        c.setRodTarget(1.0);
        for (int k = 0; k < 8000; k++) {
            c.step(0.005);   // 40 s after scram
        }
        check("AZ-5 shuts the fission down (fission < 0.5)",
            c.fissionPower() < 0.5,
            String.format("fission=%.4f", c.fissionPower()));
    }

    /**
     * 4. The void coefficient is POSITIVE: raising the void fraction raises k_eff.
     * This is the RBMK's fatal property and it must come from the cross sections.
     */
    private static void testVoidCoefficientPositive() {
        RbmkSolver3D lo = solved(0.0, 640.0);
        RbmkSolver3D hi = solved(0.4, 640.0);
        double dk = hi.keff() - lo.keff();
        check("void coefficient is POSITIVE (k rises with void)",
            dk > 0, String.format("k(void 0.0)=%.5f, k(void 0.4)=%.5f, dk=%+.5f",
                lo.keff(), hi.keff(), dk));
    }

    /**
     * 5. The scram is POSITIVE at low insertion: over the first part of the rod
     * travel, inserting the rods RAISES k_eff — the graphite-displacer effect that
     * made AZ-5 briefly raise the power. It must come from the rod geometry.
     *
     * <p>This VARIES THE ROD and holds the core state, which is what a scram is. An
     * earlier version of this check varied the VOID with the rod pinned at 0.80, so
     * it silently re-measured check 4 and passed for the wrong reason.
     */
    private static void testPositiveScram() {
        RbmkSolver3D a = solvedRod(0.00, 0.64, 640.0);
        RbmkSolver3D b = solvedRod(0.20, 0.64, 640.0);
        double dk = b.keff() - a.keff();
        check("scram is POSITIVE at low insertion (graphite displacer)",
            dk > 0, String.format("k(rod 0.00)=%.5f, k(rod 0.20)=%.5f, dk=%+.5f",
                a.keff(), b.keff(), dk));
    }

    /** 6. After a scram the reactor does not read zero: decay heat holds a floor. */
    private static void testDecayHeatFloor() {
        UnifiedReactorCore c = new UnifiedReactorCore();
        for (int k = 0; k < 400; k++) {
            c.step(0.05);   // 20 s: reach a steady state
        }
        c.setRodTarget(1.0);
        for (int k = 0; k < 1200; k++) {
            c.step(0.05);   // 60 s after scram
        }
        double total = c.power();
        check("decay heat holds a floor after scram (total > 2 % of nominal)",
            total > 0.02 && c.fissionPower() < 0.25,
            String.format("fission=%.4f, total=%.4f", c.fissionPower(), total));
    }

    private static RbmkSolver3D solved(double voidFraction, double fuelTempC) {
        RbmkSolver3D s = new RbmkSolver3D();
        s.setOmega(RbmkSolver3D.SAFE_OMEGA);
        s.setCoreState(voidFraction, fuelTempC);
        s.setRodInsertion(RbmkTransientSolver.NOMINAL_INSERTION);
        s.solve(3000, 1e-11);
        return s;
    }

    /** Solve at a given ROD insertion with the core state held fixed. */
    private static RbmkSolver3D solvedRod(double rodInsertion, double voidFraction,
                                          double fuelTempC) {
        RbmkSolver3D s = new RbmkSolver3D();
        s.setOmega(RbmkSolver3D.SAFE_OMEGA);
        s.setCoreState(voidFraction, fuelTempC);
        s.setRodInsertion(rodInsertion);
        s.solve(3000, 1e-11);
        return s;
    }

    private static void check(String name, boolean ok, String detail) {
        checks++;
        if (!ok) {
            failures++;
        }
        System.out.printf("  [%s] %s%s%n", ok ? "PASS" : "FAIL", name,
            detail.isEmpty() ? "" : "  (" + detail + ")");
    }
}
