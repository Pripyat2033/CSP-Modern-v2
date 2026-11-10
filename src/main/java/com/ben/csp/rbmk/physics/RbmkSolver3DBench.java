package com.ben.csp.rbmk.physics;

/**
 * Measures the full 3D spatial solver against the Minecraft tick budget. This is
 * the number the whole question hangs on: a server has 50 ms per tick, and the
 * reactor has to be a small enough slice of that to leave room for the rest of
 * the world.
 *
 * <p>It reports two things: a cold full solve (from a flat guess to convergence —
 * what a fresh calculation costs) and a warm step (one outer iteration from the
 * previous solution — what a running reactor costs each tick, because the flux
 * barely changes between ticks).
 */
public final class RbmkSolver3DBench {

    /** SOR factor found by scanning; 1.85 nearly halves the sweeps of plain GS. */
    private static final double OMEGA = 1.85;
    private static final int INNER = 3;

    public static void main(String[] args) {
        RbmkSolver3D s = new RbmkSolver3D();
        s.setOmega(OMEGA);
        s.setInnerSweeps(INNER);
        int nodes = s.activeNodes();
        int side = 2 * s.extent() + 1;
        System.out.println("3D two-group diffusion over the real RBMK lattice:");
        System.out.printf("  mesh          : %dx%d radial x 28 axial = %d nodes%n",
            side, side, side * side * 28);
        System.out.printf("  active nodes  : %d (the 1,872 channels x 28 nodes)%n", nodes);

        // --- warm up, then time a full cold solve ---
        s.solve(400, 1e-6);
        long t0 = System.nanoTime();
        int iters = s.solve(600, 1e-7);
        long t1 = System.nanoTime();
        double coldMs = (t1 - t0) / 1e6;
        double perIter = coldMs / iters;
        System.out.printf("  cold solve    : k_eff = %.5f, %d outer iters, %.0f ms%n",
            s.keff(), iters, coldMs);
        System.out.printf("  per iteration : %.2f ms  (one 7-point sweep of %d nodes)%n",
            perIter, nodes);

        // --- warm step: one outer iteration from the converged flux ---
        int reps = 200;
        long t2 = System.nanoTime();
        for (int r = 0; r < reps; r++) {
            s.stepOnce();
        }
        long t3 = System.nanoTime();
        double warmMs = (t3 - t2) / 1e6 / reps;
        System.out.printf("  warm step     : %.2f ms  (one iteration from a near-converged flux)%n",
            warmMs);

        System.out.println();
        System.out.println("Against the 50 ms/tick budget:");
        System.out.printf("  iterations that fit in one tick : ~%d%n", (int) (50.0 / perIter));
        System.out.printf("  ticks to a full cold solve      : ~%d (%.2f s)%n",
            (int) Math.ceil(coldMs / 50.0), coldMs / 1000.0);
        System.out.printf("  a running reactor (say 6 iters) : %.1f ms/tick (%.0f%% of a tick)%n",
            6 * perIter, 6 * perIter / 50 * 100);
        System.out.printf("  flux fields   : %.2f MB (2 groups x %d nodes x 8 B)%n",
            2.0 * nodes * 8 / 1024 / 1024, nodes);
    }
}
