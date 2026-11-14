package com.ben.csp.rbmk.physics;

import com.ben.csp.rbmk.RbmkCore;
import com.ben.csp.rbmk.RbmkGeometry;
import com.ben.csp.rbmk.RbmkLattice;

import java.util.Arrays;

/**
 * The full three-dimensional two-group neutron diffusion solver over the real
 * RBMK lattice: the calculation that decides whether Minecraft can carry the
 * core at all.
 *
 * <p>This is the spatial solver of {@link DiffusionSolver} extended to the axial
 * direction, so the flux is resolved in all three dimensions — the shape that
 * the rod-worth and the accident depend on. The mesh is the physical one:
 * {@code 51 x 51} radial cells (one per 250 mm lattice pitch at 1:4) by
 * {@link RbmkCore#AXIAL_NODES} axial nodes, with the active core circle masking
 * the cells outside it. Active nodes: 1,872 channels x 28 nodes = 52,416.
 *
 * <p>Two groups, 7-point stencil, red-black Gauss-Seidel inner sweeps, fission
 * source iteration for k_eff — the same method already validated in 2D, now on
 * the mesh the world would actually have to run.
 */
public final class RbmkSolver3D {

    private final int nx;
    private final int nz;
    private final int na;
    private final int ext;
    private final int plane;      // nz * na, the stride for one x step
    private final boolean[] active;
    private final boolean[] control;   // active nodes that are control channels
    private final RbmkXsec[] cell;

    public final double[] phi1;
    public final double[] phi2;
    private double keff = 1.0;
    private int activeNodes;
    private int sweeps;
    private int innerSweeps = 3;
    private double omega = 1.0;
    private boolean diverged = false;

    /**
     * A validated SOR over-relaxation factor. It must stay below the divergence
     * threshold of the 3D two-group system, which is not a fixed number: it falls
     * as the core departs from the nominal state. Measured worst case across the
     * void (0-1.0), fuel temperature (300-900 C) and rod (0-1.0) envelope is
     * ~1.65; at 1.70 one state already fails and at 1.85 the flux goes negative
     * and k_eff returns a meaningless value. 1.5 keeps a margin below the ceiling
     * while still roughly halving the sweeps of plain Gauss-Seidel (omega = 1.0).
     */
    public static final double SAFE_OMEGA = 1.5;

    /** 1/h^2 for the mesh pitch (one lattice cell), cm^-2. */
    private static final double INV_H2 =
        1.0 / Math.pow(RbmkGeometry.LATTICE_PITCH_M * 100.0, 2.0);

    public RbmkSolver3D() {
        this.ext = (int) Math.ceil(RbmkGeometry.CHANNEL_REGION_DIAMETER_BLOCKS / 2.0);
        this.nx = 2 * ext + 1;
        this.nz = 2 * ext + 1;
        this.na = RbmkCore.AXIAL_NODES;
        this.plane = nz * na;
        int n = nx * nz * na;
        this.active = new boolean[n];
        this.control = new boolean[n];
        this.cell = new RbmkXsec[n];
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
        this.phi1 = new double[n];
        this.phi2 = new double[n];
        setCoreState(0.0, 300.0);
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

    public boolean isActive(int x, int z, int a) {
        return in(x, z, a) && active[idx(x, z, a)];
    }

    public RbmkXsec cellAt(int x, int z, int a) {
        return cell[idx(x, z, a)];
    }

    public void setCell(int x, int z, int a, RbmkXsec xs) {
        if (isActive(x, z, a)) {
            cell[idx(x, z, a)] = xs;
        }
    }

    /** Set every active node to the given core state (void fraction, fuel temp). */
    public void setCoreState(double voidFraction, double fuelTempC) {
        RbmkXsec fuel = RbmkXsec.fuelCell(voidFraction, fuelTempC);
        for (int x = -ext; x <= ext; x++) {
            for (int z = -ext; z <= ext; z++) {
                for (int a = 0; a < na; a++) {
                    int i = idx(x, z, a);
                    if (active[i]) {
                        cell[i] = fuel;
                    }
                }
            }
        }
    }

    public boolean isControl(int x, int z, int a) {
        return in(x, z, a) && control[idx(x, z, a)];
    }

    /**
     * Apply a control-rod insertion to every control channel: each axial node gets
     * the rod material {@link RbmkGeometry#rodMaterials} puts there (B4C absorber,
     * water telescope, or the graphite displacer). This is what makes the rod's
     * axial geometry — and therefore the positive scram — visible to the solver.
     */
    public void setRodInsertion(double insertion) {
        RbmkGeometry.NodeMaterial[] mats = RbmkGeometry.rodMaterials(insertion);
        RbmkXsec graphite = RbmkXsec.controlCell(RbmkXsec.controlGraphite());
        RbmkXsec water = RbmkXsec.controlCell(RbmkXsec.WATER);
        RbmkXsec b4c = RbmkXsec.controlCell(RbmkXsec.controlB4C());
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

    /** Set the number of inner Gauss-Seidel sweeps per outer iteration. */
    public void setInnerSweeps(int n) {
        this.innerSweeps = Math.max(1, n);
    }
    /** Set the SOR over-relaxation factor, clamped to the validated safe ceiling. */
    public void setOmega(double w) {
        this.omega = Math.min(w, SAFE_OMEGA);
    }

    /** True if the last solve left the flux non-physical (negative or non-finite). */
    public boolean diverged() {
        return diverged;
    }

    /** Solve for k_eff and the 3D flux shape; returns the outer iteration count. */
    public int solve(int maxOuter, double tol) {
        Arrays.fill(phi1, 1.0);
        Arrays.fill(phi2, 1.0);
        keff = 1.0;
        sweeps = 0;
        diverged = false;
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

    /**
     * One outer iteration from the current flux, without resetting it: the
     * running-reactor step. Returns the change in k_eff this iteration, so a
     * caller can converge across ticks.
     */
    public double stepOnce() {
        double srcSum = 0;
        for (int i = 0; i < active.length; i++) {
            if (active[i]) {
                srcSum += fission(i);
            }
        }
        for (int inner = 0; inner < innerSweeps; inner++) {
            sweep(true);
            sweep(false);
            sweeps += 2;
        }
        double newSum = 0;
        for (int i = 0; i < active.length; i++) {
            if (active[i]) {
                newSum += fission(i);
            }
        }
        // A negative or non-finite flux sum means the iteration has left the
        // physical domain (over-relaxation past the divergence threshold, or a
        // bad cross section). Flag it rather than returning a meaningless k_eff
        // that a caller might scale a fission source by.
        if (!(srcSum > 0) || !(newSum > 0)) {
            diverged = true;
        }
        double newK = keff * newSum / srcSum;
        if (!Double.isFinite(newK)) {
            diverged = true;
        }
        double delta = Math.abs(newK - keff);
        keff = newK;
        return delta;
    }

    private double fission(int i) {
        RbmkXsec xs = cell[i];
        return xs.nsf1() * phi1[i] + xs.nsf2() * phi2[i];
    }

    /**
     * Eigenvalue from the neutron balance evaluated with the current flux and cross
     * sections: {@code k = production / (fast removal - up-scatter + fast leakage)}.
     *
     * <p>Unlike the power-iteration {@link #keff()}, which measures how fast the flux
     * is converging and therefore lags a cross-section change by thousands of outer
     * iterations, this responds to a rod move on the very next tick — which is what a
     * live regulator must read. At convergence the two agree.
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
                    RbmkXsec xs = cell[i];
                    int n = neighbours(x, z, a);
                    prod += xs.nsf1() * phi1[i] + xs.nsf2() * phi2[i];
                    double leak = xs.d1() * INV_H2 * (n * phi1[i] - sum(phi1, x, z, a));
                    loss += xs.sr1() * phi1[i] - xs.ss21() * phi2[i] + leak;
                }
            }
        }
        return loss > 0 ? prod / loss : 0;
    }

    /** One red-black half-sweep of the coupled two-group system. */
    private void sweep(boolean red) {
        for (int x = -ext; x <= ext; x++) {
            for (int z = -ext; z <= ext; z++) {
                int base = idx(x, z, 0);
                if (base < 0) {
                    continue;
                }
                for (int a = 0; a < na; a++) {
                    if (((x + z + a) & 1) != (red ? 0 : 1)) {
                        continue;
                    }
                    int i = base + a;
                    if (!active[i]) {
                        continue;
                    }
                    RbmkXsec xs = cell[i];
                    int n = neighbours(x, z, a);

                    // The cross sections and D are in cm, but the mesh pitch is one
                    // lattice cell = 25 cm, so the Laplacian carries a 1/h^2 factor.
                    // (Treating the pitch as 1 made leakage ~h^2 = 625x too strong.)
                    double d1h = xs.d1() * INV_H2;
                    double d2h = xs.d2() * INV_H2;

                    // Thermal group: fed by down-scatter from the fast group; lost
                    // by thermal absorption and up-scatter back to fast.
                    double s2 = xs.ss12() * phi1[i];
                    double t2 = (s2 + d2h * sum(phi2, x, z, a))
                        / (xs.sa2() + xs.ss21() + n * d2h);
                    phi2[i] = phi2[i] + omega * (t2 - phi2[i]);

                    // Fast group: fed by fission and by up-scatter from thermal.
                    double s1 = fission(i) / keff + xs.ss21() * phi2[i];
                    double t1 = (s1 + d1h * sum(phi1, x, z, a)) / (xs.sr1() + n * d1h);
                    phi1[i] = phi1[i] + omega * (t1 - phi1[i]);
                }
            }
        }
    }

    private double sum(double[] phi, int x, int z, int a) {
        double s = 0;
        if (isActive(x + 1, z, a)) {
            s += phi[idx(x + 1, z, a)];
        }
        if (isActive(x - 1, z, a)) {
            s += phi[idx(x - 1, z, a)];
        }
        if (isActive(x, z + 1, a)) {
            s += phi[idx(x, z + 1, a)];
        }
        if (isActive(x, z - 1, a)) {
            s += phi[idx(x, z - 1, a)];
        }
        if (a + 1 < na && isActive(x, z, a + 1)) {
            s += phi[idx(x, z, a + 1)];
        }
        if (a - 1 >= 0 && isActive(x, z, a - 1)) {
            s += phi[idx(x, z, a - 1)];
        }
        return s;
    }

    private int neighbours(int x, int z, int a) {
        int n = 0;
        if (isActive(x + 1, z, a)) {
            n++;
        }
        if (isActive(x - 1, z, a)) {
            n++;
        }
        if (isActive(x, z + 1, a)) {
            n++;
        }
        if (isActive(x, z - 1, a)) {
            n++;
        }
        if (a + 1 < na && isActive(x, z, a + 1)) {
            n++;
        }
        if (a - 1 >= 0 && isActive(x, z, a - 1)) {
            n++;
        }
        return n;
    }

    /** The axial flux shape (group 1, averaged over the core) — for the rod model. */
    public double[] axialShape() {
        double[] shape = new double[na];
        int[] count = new int[na];
        for (int x = -ext; x <= ext; x++) {
            for (int z = -ext; z <= ext; z++) {
                for (int a = 0; a < na; a++) {
                    int i = idx(x, z, a);
                    if (active[i]) {
                        shape[a] += phi1[i];
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

    private int idx(int x, int z, int a) {
        return (x + ext) * plane + (z + ext) * na + a;
    }

    private boolean in(int x, int z, int a) {
        return x >= -ext && x <= ext && z >= -ext && z <= ext && a >= 0 && a < na;
    }
}
