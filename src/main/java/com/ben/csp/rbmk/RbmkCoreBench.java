package com.ben.csp.rbmk;

/**
 * Proof + benchmark: the core stepped as a scale-free simulation.
 *
 * <p>It reports the mesh size, the memory, and the wall-clock cost of a solve
 * step — the number that decides whether the plant can tick inside Minecraft's
 * 50 ms budget. The step here is a stand-in (axial diffusion plus a mean radial
 * coupling) whose only purpose is to price the mesh; the real solver replaces it
 * later without changing the cost class.
 */
public final class RbmkCoreBench {

    public static void main(String[] args) {
        RbmkCore core = new RbmkCore();
        int channels = core.channelCount();
        int nodes = core.nodeCount();
        int axial = RbmkCore.AXIAL_NODES;

        System.out.println("RBMK core, scale-free:");
        System.out.println("  channels          : " + channels
            + " (" + RbmkLattice.fuelCount() + " fuel + " + RbmkLattice.controlCount() + " control)");
        System.out.println("  axial nodes       : " + axial);
        System.out.println("  mesh nodes        : " + nodes);
        System.out.println("  core state memory : " + (core.memoryBytes() / 1024) + " KiB");
        System.out.println("  if one node = one block, blocks needed: " + nodes);
        System.out.println("  (that is the cost we AVOID by keeping the core as data)");

        double[] flux = new double[nodes];
        double[] next = new double[nodes];
        for (int i = 0; i < nodes; i++) {
            flux[i] = 1.0;
        }

        int steps = 200;
        int warm = 20;
        for (int s = 0; s < warm; s++) {
            step(core, flux, next, channels, axial, 0.1);
            System.arraycopy(next, 0, flux, 0, nodes);
        }
        long t0 = System.nanoTime();
        for (int s = 0; s < steps; s++) {
            step(core, flux, next, channels, axial, 0.1);
            System.arraycopy(next, 0, flux, 0, nodes);
        }
        long t1 = System.nanoTime();
        double msPerStep = (t1 - t0) / 1e6 / steps;
        System.out.printf("  solve step        : %.3f ms  (budget: 50 ms/tick)%n", msPerStep);
        System.out.printf("  headroom          : %.1fx%n", 50.0 / Math.max(msPerStep, 1e-9));
    }

    /** Axial diffusion + a cheap mean radial coupling. A timing proxy for the mesh. */
    private static void step(RbmkCore core, double[] flux, double[] next,
                             int channels, int axial, double alpha) {
        double mean = 0;
        for (double v : flux) {
            mean += v;
        }
        mean /= flux.length;
        int n = flux.length;
        for (int i = 0; i < n; i++) {
            int c = i / axial;
            int z = i % axial;
            double lap = 0;
            if (z > 0) {
                lap += flux[i - 1] - flux[i];
            }
            if (z < axial - 1) {
                lap += flux[i + 1] - flux[i];
            }
            lap += 0.05 * (mean - flux[i]); // stand-in radial coupling
            next[i] = flux[i] + alpha * lap;
        }
    }
}
