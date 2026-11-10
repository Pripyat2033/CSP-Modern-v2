package com.ben.csp.rbmk.physics;

import com.ben.csp.rbmk.RbmkGeometry;
import com.ben.csp.rbmk.RbmkLattice;

import java.util.Arrays;

/**
 * The spatial two-group neutron diffusion eigenvalue problem over the real RBMK
 * lattice: the calculation that gives the effective multiplication factor k_eff
 * and the flux shape. This is the heart of a reactor physics code.
 *
 * <p>For each lattice cell (one cell = one channel, pitch = 1 in these units):
 * <pre>
 *   -D1 lap(phi1) + Sr1 phi1 = (1/k)(nsf1 phi1 + nsf2 phi2)
 *   -D2 lap(phi2) + Sa2  phi2 = Ss12 phi1
 * </pre>
 * Discretised with the 5-point stencil, the update for a cell with N active
 * neighbours is {@code phi_i = (S + D * sum(phi_j)) / (Sigma + N*D)}. Solved by
 * fission-source iteration with red-black Gauss-Seidel inner sweeps — the standard,
 * simple, robust route, and trivially stable.
 */
public final class DiffusionSolver {

    private final int nx;
    private final int nz;
    private final int ext;
    private final boolean[] active;
    private final boolean[] control;
    private final RbmkXsec[] cell;

    public final double[] phi1;
    public final double[] phi2;
    private double keff = 1.0;
    private int channels;

    public DiffusionSolver() {
        this.ext = (int) Math.ceil(RbmkGeometry.CHANNEL_REGION_DIAMETER_BLOCKS / 2.0);
        this.nx = 2 * ext + 1;
        this.nz = 2 * ext + 1;
        this.active = new boolean[nx * nz];
        this.control = new boolean[nx * nz];
        this.cell = new RbmkXsec[nx * nz];
        for (RbmkLattice.Channel c : RbmkLattice.channels()) {
            int i = idx(c.x(), c.z());
            active[i] = true;
            control[i] = c.control();
            channels++;
        }
        setCoreState(0.0, 300.0);
        this.phi1 = new double[nx * nz];
        this.phi2 = new double[nx * nz];
    }

    public int extent() {
        return ext;
    }

    public int channels() {
        return channels;
    }

    public double keff() {
        return keff;
    }

    public boolean isActive(int x, int z) {
        return in(x, z) && active[idx(x, z)];
    }

    public boolean isControl(int x, int z) {
        return in(x, z) && control[idx(x, z)];
    }

    public RbmkXsec cellAt(int x, int z) {
        return cell[idx(x, z)];
    }

    public void setCell(int x, int z, RbmkXsec xs) {
        if (isActive(x, z)) {
            cell[idx(x, z)] = xs;
        }
    }

    /** Set every active cell to the given core state (void fraction, fuel temp). */
    public void setCoreState(double voidFraction, double fuelTempC) {
        for (int x = -ext; x <= ext; x++) {
            for (int z = -ext; z <= ext; z++) {
                if (!isActive(x, z)) {
                    continue;
                }
                setCell(x, z, isControl(x, z)
                    ? RbmkXsec.controlCell(RbmkXsec.WATER)
                    : RbmkXsec.fuelCell(voidFraction, fuelTempC));
            }
        }
    }

    /** Solve for k_eff and the flux shape; returns the outer iteration count. */
    public int solve(int maxOuter, double tol) {
        Arrays.fill(phi1, 1.0);
        Arrays.fill(phi2, 1.0);
        keff = 1.0;
        int it = 0;
        for (; it < maxOuter; it++) {
            double srcSum = 0;
            for (int i = 0; i < active.length; i++) {
                if (active[i]) {
                    srcSum += fission(i);
                }
            }
            for (int inner = 0; inner < 3; inner++) {
                sweep(true);
                sweep(false);
            }
            double newSum = 0;
            for (int i = 0; i < active.length; i++) {
                if (active[i]) {
                    newSum += fission(i);
                }
            }
            double newK = keff * newSum / srcSum;
            double delta = Math.abs(newK - keff);
            keff = newK;
            if (delta < tol) {
                it++;
                break;
            }
        }
        return it;
    }

    private double fission(int i) {
        RbmkXsec xs = cell[i];
        return xs.nsf1() * phi1[i] + xs.nsf2() * phi2[i];
    }

    /** One red-black half-sweep of the coupled two-group system. */
    private void sweep(boolean red) {
        for (int x = -ext; x <= ext; x++) {
            for (int z = -ext; z <= ext; z++) {
                if (((x + z) & 1) != (red ? 0 : 1)) {
                    continue;
                }
                if (!isActive(x, z)) {
                    continue;
                }
                int i = idx(x, z);
                RbmkXsec xs = cell[i];
                int n = activeNeighbours(x, z);

                double s2 = xs.ss12() * phi1[i];
                phi2[i] = (s2 + xs.d2() * neighbourFlux(phi2, x, z)) / (xs.sa2() + n * xs.d2());

                double s1 = fission(i) / keff;
                phi1[i] = (s1 + xs.d1() * neighbourFlux(phi1, x, z)) / (xs.sr1() + n * xs.d1());
            }
        }
    }

    private double neighbourFlux(double[] phi, int x, int z) {
        double s = 0;
        if (isActive(x + 1, z)) {
            s += phi[idx(x + 1, z)];
        }
        if (isActive(x - 1, z)) {
            s += phi[idx(x - 1, z)];
        }
        if (isActive(x, z + 1)) {
            s += phi[idx(x, z + 1)];
        }
        if (isActive(x, z - 1)) {
            s += phi[idx(x, z - 1)];
        }
        return s;
    }

    private int activeNeighbours(int x, int z) {
        int n = 0;
        if (isActive(x + 1, z)) {
            n++;
        }
        if (isActive(x - 1, z)) {
            n++;
        }
        if (isActive(x, z + 1)) {
            n++;
        }
        if (isActive(x, z - 1)) {
            n++;
        }
        return n;
    }

    public double meanFlux1() {
        double s = 0;
        int n = 0;
        for (int i = 0; i < active.length; i++) {
            if (active[i]) {
                s += phi1[i];
                n++;
            }
        }
        return n == 0 ? 0 : s / n;
    }

    /** Peak/mean flux ratio: a coarse measure of flux flattening. */
    public double peakOverMean1() {
        double m = meanFlux1();
        double peak = 0;
        for (int i = 0; i < active.length; i++) {
            if (active[i]) {
                peak = Math.max(peak, phi1[i]);
            }
        }
        return m == 0 ? 0 : peak / m;
    }

    private int idx(int x, int z) {
        return (x + ext) * nz + (z + ext);
    }

    private boolean in(int x, int z) {
        return x >= -ext && x <= ext && z >= -ext && z <= ext;
    }
}
