package com.ben.csp.rbmk.physics;

/**
 * Self-test for {@link SpatialReactorCore} — the core driven by the full 3D
 * two-group diffusion solver. This is the test that says the spatially resolved
 * RBMK is a real, controllable reactor, not just a solver that runs:
 *
 * <ul>
 *   <li>it holds a steady reaction at nominal power (the АР can hold it);</li>
 *   <li>the 3D feedbacks have the right sign and magnitude (positive void,
 *       negative Doppler), matching {@link ReactivityModel};</li>
 *   <li>a manual rod withdrawal raises power on a positive period;</li>
 *   <li>AZ-5 shuts the core down;</li>
 *   <li>one solver iteration per tick costs a small fraction of the tick budget.</li>
 * </ul>
 */
public final class SpatialReactorCoreSelfTest {

    private static int checks = 0;

    private static void check(boolean ok, String what) {
        checks++;
        if (!ok) {
            throw new AssertionError("FAILED: " + what);
        }
    }

    public static void main(String[] args) {
        // --- 1. the АР holds a steady reaction at nominal power ---
        SpatialReactorCore core = new SpatialReactorCore();
        core.setAutoRegulator(true);
        for (int i = 0; i < 4000; i++) {
            core.step(0.05); // 200 s to settle
        }
        double min = 9, max = 0, sum = 0;
        int n = 0;
        for (int i = 0; i < 2400; i++) { // 120 s window
            core.step(0.05);
            min = Math.min(min, core.power());
            max = Math.max(max, core.power());
            sum += core.power();
            n++;
        }
        double mean = sum / n;
        check(Math.abs(mean - 1.0) < 0.05,
            "the 3D core holds nominal power on average (" + fmt(mean) + ")");
        check(max - min < 0.5,
            "the АР holds the power steady, not hunting (" + fmt(min) + ".." + fmt(max) + ")");
        check(core.fuelTempC() > 400 && core.fuelTempC() < 900,
            "the fuel sits at a physical temperature (" + Math.round(core.fuelTempC()) + " C)");
        check(Math.abs(core.reactivity()) < 5e-3,
            "the settled core is near-critical (" + fmt(core.reactivity()) + ")");

        // --- 2. the 3D feedbacks: positive void, negative Doppler ---
        double kBase = keff(0.0, 300.0);
        double kHot = keff(0.0, 1000.0);
        double kSteam = keff(0.5, 300.0);
        check(kHot < kBase,
            "Doppler is negative in 3D: k(hot) " + fmt(kHot) + " < k(cold) " + fmt(kBase));
        check(kSteam > kBase,
            "void adds reactivity in 3D: k(void) " + fmt(kSteam) + " > k(cold) " + fmt(kBase));
        double dopplerPerK = (kHot - kBase) / kBase / 700.0;
        check(dopplerPerK < 0 && dopplerPerK > -1e-4,
            "the 3D Doppler coefficient is physical (" + String.format("%.2e", dopplerPerK)
                + " /K, reference " + String.format("%.1e", ReactivityModel.DOPPLER_COEFF) + ")");

        // --- 3. manual withdrawal raises power on a positive period ---
        SpatialReactorCore manual = new SpatialReactorCore();
        for (int i = 0; i < 400; i++) {
            manual.step(0.05);
        }
        manual.setRodTarget(manual.insertion() - 0.05);
        for (int i = 0; i < 400; i++) {
            manual.step(0.05);
        }
        check(manual.power() > 1.1,
            "withdrawing rods raises power (" + fmt(manual.power()) + ")");
        check(manual.reactivity() > 0,
            "the withdrawn core is super-critical (" + fmt(manual.reactivity()) + ")");

        // --- 4. AZ-5 shuts the core down ---
        SpatialReactorCore scram = new SpatialReactorCore();
        scram.setAutoRegulator(true);
        for (int i = 0; i < 800; i++) {
            scram.step(0.05);
        }
        scram.setRodTarget(1.0);
        for (int i = 0; i < 2000; i++) {
            scram.step(0.05);
        }
        check(scram.insertion() > 0.99, "AZ-5 drives the rods fully in");
        check(scram.power() < 0.05, "the core is shut down after AZ-5 (" + fmt(scram.power()) + ")");

        // --- 5. the live spatial cost fits the tick ---
        check(core.lastSolveMs() < 25.0,
            "one 3D iteration per tick fits the budget (" + fmt(core.lastSolveMs()) + " ms)");

        System.out.println("SpatialReactorCoreSelfTest: " + checks + " checks passed.");
    }

    private static double keff(double voidFraction, double fuelTempC) {
        RbmkSolver3D s = new RbmkSolver3D();
        s.setCoreState(voidFraction, fuelTempC);
        s.setRodInsertion(0.80);
        s.solve(800, 1e-7);
        return s.keff();
    }

    private static String fmt(double v) {
        return String.format("%.4f", v);
    }
}
