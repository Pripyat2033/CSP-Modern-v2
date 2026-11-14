package com.ben.csp.rbmk.physics;

import com.ben.csp.rbmk.RbmkCore;
import com.ben.csp.rbmk.RbmkGeometry;
import com.ben.csp.rbmk.RbmkLattice;

import java.util.Arrays;

/**
 * The three-dimensional G-group neutron diffusion solver — {@link RbmkSolver3D}
 * generalised from two energy groups to any number, with the full scattering
 * matrix between them and a fission spectrum.
 *
 * <p>The point of item 3: the RBMK is a thermal reactor whose behaviour depends
 * on a fine energy structure (resonance self-shielding, the thermal spectrum).
 * Two groups capture that coarsely; the engine should not hard-code two. The
 * group count is a property of the DATA, not the solver, so the same code runs a
 * 2-group set (exactly reproducing {@link RbmkSolver3D}) or an 8-group set.
 *
 * <p>For G = 2 the update is algebraically identical to {@link RbmkSolver3D}
 * (same thermal-then-fast order, same fission source), which is what
 * {@code RbmkSolverMGSelfTest} checks against the two-group solver.
 *
 * <p>The mesh is the physical one: one radial cell per 250 mm lattice pitch at
 * 1:4, by {@link RbmkCore#AXIAL_NODES} axial nodes, active cells masking the
 * circle outside the core. Red-black Gauss-Seidel inner sweeps, fission-source
 * iteration for k_eff.
 */
public final class RbmkSolverMG {

    private final int groups;
    private final int ext;
    private final int na;
    private final int plane;
    private final boolean[] active;
    private final boolean[] control;
    private final GroupData[] cell;
    private final double[][] phi;

    private double keff = 1.0;
    private int activeNodes;
    private int sweeps;
    private int innerSweeps = 3;
    private double omega = 1.0;

    /** 1/h^2 for the mesh pitch (one lattice cell), cm^-2. */
    private static final double INV_H2 =
        1.0 / Math.pow(RbmkGeometry.LATTICE_PITCH_M * 100.0, 2.0);

    public RbmkSolverMG(int groups) {
        if (groups < 2) {
            throw new IllegalArgumentException("need at least two groups");
        }
        this.groups = groups;
        this.ext = (int) Math.ceil(RbmkGeometry.CHANNEL_REGION_DIAMETER_CELLS / 2.0);
        int nx = 2 * ext + 1;
        int nz = 2 * ext + 1;
        this.na = RbmkCore.AXIAL_NODES;
        this.plane = nz * na;
        int n = nx * nz * na;
        this.active = new boolean[n];
        this.control = new boolean[n];
        this.cell = new GroupData[n];
        this.phi = new double[groups][n];
        for (RbmkLattice.Channel c : RbmkLattice.channels()) {
            for (int a = 0; a < na; a++) {
                int i = idx(c.x(), c.z(), a);
                active[i] = true;
                if (c.control()) {
                    control[i] = true;
                }
                activeNodes++;
            }
        }
        setCoreState(0.0, 300.0);
    }

    public int groups() {
        return groups;
    }

    public int extent() {
        return ext;
    }

    public int activeNodes() {
        return activeNodes;
    }

    public double keff() {
        return keff;
    }

    public int sweeps() {
        return sweeps;
    }

    public double[] flux(int g) {
        return phi[g];
    }

    /** Total flux per node (sum over groups) — the shape a rod worth sees. */
    public double[] axialShape() {
        double[] shape = new double[na];
        int[] count = new int[na];
        for (int x = -ext; x <= ext; x++) {
            for (int z = -ext; z <= ext; z++) {
                for (int a = 0; a < na; a++) {
                    int i = idx(x, z, a);
                    if (active[i]) {
                        double t = 0;
                        for (int g = 0; g < groups; g++) {
                            t += phi[g][i];
                        }
                        shape[a] += t;
                        count[a]++;
                    }
                }
            }
        }
        for (int a = 0; a < na; a++) {
            if (count[a] > 0) {
                shape[a] /= count[a];
            }
        }
        return shape;
    }

    private GroupData widen(RbmkXsec xs) {
        return groups == 2 ? GroupData.of(xs) : GroupData.repeat(xs, groups);
    }

    /** Set every active node to the given core state (void fraction, fuel temp). */
    public void setCoreState(double voidFraction, double fuelTempC) {
        GroupData fuel = widen(RbmkXsec.fuelCell(voidFraction, fuelTempC));
        for (int i = 0; i < active.length; i++) {
            if (active[i]) {
                cell[i] = fuel;
            }
        }
    }

    /** Apply a control-rod insertion to every control channel (axial geometry). */
    public void setRodInsertion(double insertion) {
        RbmkGeometry.NodeMaterial[] mats = RbmkGeometry.rodMaterials(insertion);
        GroupData graphite = widen(RbmkXsec.controlCell(RbmkXsec.controlGraphite()));
        GroupData water = widen(RbmkXsec.controlCell(RbmkXsec.WATER));
        GroupData b4c = widen(RbmkXsec.controlCell(RbmkXsec.controlB4C()));
        for (int i = 0; i < control.length; i++) {
            if (!control[i]) {
                continue;
            }
            int a = i % na;
            cell[i] = switch (mats[a]) {
                case B4C -> b4c;
                case GRAPHITE -> graphite;
                case WATER -> water;
            };
        }
    }

    public void setInnerSweeps(int n) {
        this.innerSweeps = Math.max(1, n);
    }

    public void setOmega(double w) {
        this.omega = w;
    }

    /** Solve for k_eff and the flux shape; returns the outer iteration count. */
    public int solve(int maxOuter, double tol) {
        for (int g = 0; g < groups; g++) {
            Arrays.fill(phi[g], 1.0);
        }
        keff = 1.0;
        sweeps = 0;
        int it = 0;
        for (; it < maxOuter; it++) {
            double delta = stepOnce();
            if (delta < tol) {
                it++;
                break;
            }
        }
        return it;
    }

    /** One outer iteration from the current flux; returns the change in k_eff. */
    public double stepOnce() {
        double srcSum = totalFission();
        for (int inner = 0; inner < innerSweeps; inner++) {
            sweep(true);
            sweep(false);
            sweeps += 2;
        }
        double newSum = totalFission();
        double newK = keff * newSum / srcSum;
        double delta = Math.abs(newK - keff);
        keff = newK;
        return delta;
    }

    private double totalFission() {
        double s = 0;
        for (int i = 0; i < active.length; i++) {
            if (active[i]) {
                s += fission(i);
            }
        }
        return s;
    }

    private double fission(int i) {
        GroupData xs = cell[i];
        double s = 0;
        for (int g = 0; g < groups; g++) {
            s += xs.nsf(g) * phi[g][i];
        }
        return s;
    }

    /**
     * Eigenvalue from the neutron balance at the current flux: total production
     * over total loss (absorption plus leakage). At convergence it equals
     * {@link #keff()}; unlike the power iteration it responds to a material change
     * on the next step, which is what a live regulator reads.
     */
    public double balanceKeff() {
        double prod = 0;
        double loss = 0;
        for (int x = -ext; x <= ext; x++) {
            for (int z = -ext; z <= ext; z++) {
                for (int a = 0; a < na; a++) {
                    int i = idx(x, z, a);
                    if (!active[i]) {
                        continue;
                    }
                    GroupData xs = cell[i];
                    int n = neighbours(x, z, a);
                    prod += fission(i);
                    for (int g = 0; g < groups; g++) {
                        double leak = xs.d(g) * INV_H2 * (n * phi[g][i] - sum(phi[g], x, z, a));
                        loss += xs.sa(g) * phi[g][i] + leak;
                    }
                }
            }
        }
        return loss > 0 ? prod / loss : 0;
    }

    /** One red-black half-sweep of the coupled G-group system. */
    private void sweep(boolean red) {
        for (int x = -ext; x <= ext; x++) {
            for (int z = -ext; z <= ext; z++) {
                int base = idx(x, z, 0);
                for (int a = 0; a < na; a++) {
                    if (((x + z + a) & 1) != (red ? 0 : 1)) {
                        continue;
                    }
                    int i = base + a;
                    if (!active[i]) {
                        continue;
                    }
                    GroupData xs = cell[i];
                    int n = neighbours(x, z, a);
                    // Thermal-first order (highest group index down). The fission
                    // source is recomputed per group from the current flux, so the
                    // fast group sees the just-updated thermal flux — matching
                    // RbmkSolver3D exactly at G=2.
                    for (int g = groups - 1; g >= 0; g--) {
                        double dh = xs.d(g) * INV_H2;
                        double src = xs.chi(g) * fission(i) / keff + xs.inScatter(g, phi, i)
                            + dh * sum(phi[g], x, z, a);
                        double denom = xs.removal(g) + n * dh;
                        double t = src / denom;
                        phi[g][i] = phi[g][i] + omega * (t - phi[g][i]);
                    }
                }
            }
        }
    }

    private double sum(double[] p, int x, int z, int a) {
        double s = 0;
        if (isActive(x + 1, z, a)) {
            s += p[idx(x + 1, z, a)];
        }
        if (isActive(x - 1, z, a)) {
            s += p[idx(x - 1, z, a)];
        }
        if (isActive(x, z + 1, a)) {
            s += p[idx(x, z + 1, a)];
        }
        if (isActive(x, z - 1, a)) {
            s += p[idx(x, z - 1, a)];
        }
        if (a + 1 < na && isActive(x, z, a + 1)) {
            s += p[idx(x, z, a + 1)];
        }
        if (a - 1 >= 0 && isActive(x, z, a - 1)) {
            s += p[idx(x, z, a - 1)];
        }
        return s;
    }

    private int neighbours(int x, int z, int a) {
        int n = 0;
        if (isActive(x + 1, z, a)) n++;
        if (isActive(x - 1, z, a)) n++;
        if (isActive(x, z + 1, a)) n++;
        if (isActive(x, z - 1, a)) n++;
        if (a + 1 < na && isActive(x, z, a + 1)) n++;
        if (a - 1 >= 0 && isActive(x, z, a - 1)) n++;
        return n;
    }

    private boolean isActive(int x, int z, int a) {
        return in(x, z, a) && active[idx(x, z, a)];
    }

    private int idx(int x, int z, int a) {
        return (x + ext) * plane + (z + ext) * na + a;
    }

    private boolean in(int x, int z, int a) {
        return x >= -ext && x <= ext && z >= -ext && z <= ext && a >= 0 && a < na;
    }
}
