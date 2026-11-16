package com.ben.csp.rbmk.physics;

import com.ben.csp.rbmk.RbmkCore;
import com.ben.csp.rbmk.RbmkLattice;

/**
 * PROBE — does the HOMOGENISED core exaggerate the void worth?
 *
 * <p>The Director's diagnosis (2026-11-15): a real RBMK-1000 behaved like 1,661
 * individual channels, each with its own void and temperature. The model, by
 * contrast, forces EVERY channel to the same void ({@link RbmkSolver3D#setCoreState}).
 * This probe measures the whole-core void coefficient {@code dk/d(void)} the same
 * way {@code DerivedVoidSelfTest} does — the eigenvalue at a uniform baseline void
 * of 0.30 and 0.40 — but over four different ways of building the state, all with
 * the same core-AVERAGE void:
 *
 * <pre>
 *   A UNIFORM   every node the same void          (today's model)
 *   B CHANNEL   a fixed channel-to-channel spread about the mean
 *   C AXIAL     a rising (bottom-to-top) axial profile about the mean
 *   D BOTH      channel spread and axial profile
 * </pre>
 *
 * <p>If the coefficient changes when the channels are allowed to differ, the
 * homogenisation is the error and the anchor is hiding it. This probe only PRINTS
 * — it asserts nothing, so it cannot fail a build while the diagnosis is unproven.
 *
 * <p>Run: {@code java -cp <out>:<gson>:src/main/resources ...HomogenisationProbe}
 */
public final class HomogenisationProbe {

    private static final int NA = RbmkCore.AXIAL_NODES;
    private static final double SPREAD = 0.15;   // channel-to-channel void spread, +/-
    private static final double AMP = 0.25;      // axial void swing, peak-to-peak / 2
    private static final double TILT = 0.20;     // side-to-side void tilt (flux-tilt probe)

    public static void main(String[] args) {
        System.out.println("=== HomogenisationProbe: does the lump exaggerate the void worth? ===");
        System.out.printf("channels=%d, axial nodes=%d, spread=%.2f, axial amp=%.2f%n%n",
            RbmkLattice.channels().size(), NA, SPREAD, AMP);

        String[] names = {"A UNIFORM (today)", "B CHANNEL spread", "C AXIAL profile", "D BOTH"};
        double base = 0.0;
        System.out.printf("%-22s %10s %10s %12s %10s%n",
            "state", "k(0.30)", "k(0.40)", "dk/dvoid", "vs A");
        for (int mode = 0; mode < 4; mode++) {
            double k30 = keff(mode, 0.30);
            double k40 = keff(mode, 0.40);
            double dk = (k40 - k30) / 0.1;
            if (mode == 0) {
                base = dk;
            }
            System.out.printf("%-22s %10.5f %10.5f %+12.4f %9.2fx%n",
                names[mode], k30, k40, dk, dk / base);
        }

        System.out.println();
        fluxTiltProbe();
    }

    /**
     * Void one side of the core more than the other and see whether the flux tilts
     * toward the LESS voided (more reactive) side. A homogenised core cannot show
     * this redistribution; a heterogeneous one can.
     */
    private static void fluxTiltProbe() {
        RbmkSolver3D s = build(4, 0.35);   // mode 4 = side-to-side tilt
        s.solve(1200, 1e-10);
        int ext = s.extent();
        // phi1 is public and indexed by idx(x,z,a); re-derive the index directly.
        double sumL = 0, sumR = 0;
        int cL = 0, cR = 0;
        for (RbmkLattice.Channel c : RbmkLattice.channels()) {
            for (int a = 0; a < NA; a++) {
                int i = index(c.x(), c.z(), a, ext);
                if (c.x() < 0) { sumL += s.phi1[i]; cL++; }
                else if (c.x() > 0) { sumR += s.phi1[i]; cR++; }
            }
        }
        double ml = sumL / Math.max(1, cL), mr = sumR / Math.max(1, cR);
        System.out.printf("flux-tilt probe (void higher on +x side): mean phi1  -x=%.4f  +x=%.4f"
            + "  -> flux tilts %s%n", ml, mr, mr > ml ? "toward +x (unexpected)" : "toward -x (the less-voided side, as physics expects)");
    }

    private static int idxUnused() {
        return 0;
    }

    /** The solver's node index: (x+ext)*plane + (z+ext)*na + a, plane = nz*na. */
    private static int index(int x, int z, int a, int ext) {
        int nz = 2 * ext + 1;
        int plane = nz * NA;
        return (x + ext) * plane + (z + ext) * NA + a;
    }

    private static double keff(int mode, double v) {
        RbmkSolver3D s = build(mode, v);
        s.solve(1500, 1e-10);
        return s.keff();
    }

    private static RbmkSolver3D build(int mode, double v) {
        RbmkSolver3D s = new RbmkSolver3D();
        s.setOmega(RbmkSolver3D.SAFE_OMEGA);
        s.setInnerSweeps(3);
        for (RbmkLattice.Channel c : RbmkLattice.channels()) {
            if (c.control()) {
                continue;
            }
            for (int a = 0; a < NA; a++) {
                double vv = clamp01(voidFor(mode, c.x(), c.z(), a, v, s.extent()));
                s.setCell(c.x(), c.z(), a, RbmkXsec.fuelCell(vv, 640.0));
            }
        }
        s.setRodInsertion(RbmkTransientSolver.NOMINAL_INSERTION);
        return s;
    }

    /** The void at a node for a given mode; every mode has core-mean void = v. */
    private static double voidFor(int mode, int x, int z, int a, double v, int ext) {
        double chan = SPREAD * channelNoise(x, z);
        double axial = AMP * (2.0 * a / (NA - 1.0) - 1.0);   // bottom low, top high
        double tilt = TILT * (x / (double) ext);
        return switch (mode) {
            case 0 -> v;
            case 1 -> v + chan;
            case 2 -> v + axial;
            case 3 -> v + chan + axial;
            case 4 -> v + tilt;
            default -> v;
        };
    }

    /** A deterministic channel-to-channel wobble in [-1, 1], mean ~0. */
    private static double channelNoise(int x, int z) {
        int h = x * 73856093 ^ z * 19349663;
        h ^= (h >>> 13);
        h *= 1274126177;
        h ^= (h >>> 16);
        return ((h & 0xFFFF) / 65535.0) * 2.0 - 1.0;
    }

    private static double clamp01(double x) {
        return x < 0 ? 0 : Math.min(x, 1.0);
    }

    private HomogenisationProbe() {
    }
}
