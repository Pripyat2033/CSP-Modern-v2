package com.ben.csp.rbmk.physics;

/**
 * Self-test for the G-group solver {@link RbmkSolverMG} (item 3: generalise the
 * core from two energy groups to any number).
 *
 * <p>What is PROVEN here, and what is not:
 * <ul>
 *   <li><b>G=2 reproduces the two-group solver exactly.</b> Stepped the same
 *       number of times from the same state, {@code RbmkSolverMG(2)} matches
 *       {@link RbmkSolver3D} in both k_eff and the flux field to machine
 *       precision. This is the point the generalisation MUST reduce to the thing
 *       it generalises, and it does.</li>
 *   <li><b>G&gt;2 runs, converges and is stable</b>, and its k_eff is close to the
 *       two-group value. It is NOT bit-exact: the block-repeat data is only an
 *       exact equivalent AT convergence, and a Gauss-Seidel step updates the
 *       groups in sequence, so at a finite iteration count the two differ by a
 *       small ordering residual. That residual is reported, not hidden.</li>
 *   <li><b>The rods still work</b> — inserting them lowers k_eff.</li>
 *   <li><b>The balance eigenvalue agrees</b> with the power-iteration eigenvalue.</li>
 * </ul>
 */
public final class RbmkSolverMGSelfTest {

    private static int checks = 0;
    private static final int STEPS = 400;

    private static void check(boolean ok, String what) {
        checks++;
        if (!ok) {
            throw new AssertionError("FAILED: " + what);
        }
    }

    private static RbmkSolverMG stepped(int groups, double insertion, int steps) {
        RbmkSolverMG s = new RbmkSolverMG(groups);
        s.setCoreState(0.0, 300.0);
        s.setRodInsertion(insertion);
        s.solve(steps, 0.0);   // tol 0 => exactly `steps` outer iterations
        return s;
    }

    public static void main(String[] args) {
        // --- G = 2 must reproduce the two-group solver EXACTLY ---
        RbmkSolver3D two = new RbmkSolver3D();
        two.setCoreState(0.0, 300.0);
        two.setRodInsertion(0.80);
        two.solve(STEPS, 0.0);

        RbmkSolverMG mg2 = stepped(2, 0.80, STEPS);
        check(Math.abs(two.keff() - mg2.keff()) < 1e-12,
            "G=2 reproduces the two-group solver exactly (k3D=" + two.keff()
            + " kMG=" + mg2.keff() + ")");

        double maxFlux = 0;
        for (int i = 0; i < two.phi1.length; i++) {
            maxFlux = Math.max(maxFlux, Math.abs(two.phi1[i] - mg2.flux(0)[i]));
            maxFlux = Math.max(maxFlux, Math.abs(two.phi2[i] - mg2.flux(1)[i]));
        }
        check(maxFlux < 1e-12, "the G=2 flux field matches node-for-node (" + maxFlux + ")");

        // --- G > 2 runs, is physical, and is close to the two-group value ---
        RbmkSolverMG mg4 = stepped(4, 0.80, STEPS);
        RbmkSolverMG mg8 = stepped(8, 0.80, STEPS);
        check(Double.isFinite(mg4.keff()) && mg4.keff() > 0.5 && mg4.keff() < 2.0,
            "a 4-group solve is physical (" + mg4.keff() + ")");
        check(Double.isFinite(mg8.keff()) && mg8.keff() > 0.5 && mg8.keff() < 2.0,
            "an 8-group solve is physical (" + mg8.keff() + ")");
        double d4 = Math.abs(mg4.keff() - two.keff());
        double d8 = Math.abs(mg8.keff() - two.keff());
        check(d4 < 1e-3, "4 groups agree with 2 groups within the ordering residual ("
            + d4 + ")");
        check(d8 < 1e-3, "8 groups agree with 2 groups within the ordering residual ("
            + d8 + ")");

        // --- the rods still work, in every group count ---
        for (int g : new int[] {2, 4, 8}) {
            RbmkSolverMG out = stepped(g, 0.0, STEPS);   // rods fully out
            RbmkSolverMG in = stepped(g, 1.0, STEPS);    // rods fully in
            check(in.balanceKeff() < out.balanceKeff(),
                "G=" + g + ": inserting the rods lowers k_eff (" + out.balanceKeff()
                + " -> " + in.balanceKeff() + ")");
        }

        // --- the balance eigenvalue agrees with the power iteration ---
        RbmkSolverMG bal = new RbmkSolverMG(2);
        bal.setCoreState(0.0, 300.0);
        bal.setRodInsertion(0.80);
        bal.solve(1200, 1e-8);
        check(Math.abs(bal.balanceKeff() - bal.keff()) < 5e-3,
            "the balance eigenvalue tracks the power-iteration k_eff ("
            + bal.balanceKeff() + " vs " + bal.keff() + ")");

        System.out.println("RbmkSolverMGSelfTest: " + checks + " checks passed.");
    }
}
