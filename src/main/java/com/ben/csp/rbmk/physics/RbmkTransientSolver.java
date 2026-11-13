package com.ben.csp.rbmk.physics;

import com.ben.csp.rbmk.RbmkCore;
import com.ben.csp.rbmk.RbmkGeometry;
import com.ben.csp.rbmk.RbmkLattice;

/**
 * Time-dependent two-group neutron diffusion over the real RBMK lattice — the
 * flux IS the state. There is no point kinetics here and no lumped reactivity:
 * the power evolution emerges from the spatial flux solution and the per-node
 * delayed-neutron precursors, so a spatial disturbance (a rod's displacer, a void
 * pocket) moves the flux where the physics puts it, not where a fitted term says.
 *
 * <p>Equations (two energy groups, g = 1 fast / 2 thermal, with L = the prompt
 * generation time and the six delayed groups C_k tracked per node):
 * <pre>
 *   (1/L) dphi1/dt = D1*sum(phi1_n) - Sr1*phi1 + (1-b) Sf + sum_k lam_k C_k
 *   (1/L) dphi2/dt = D2*sum(phi2_n) - Sa2*phi2 + Ss12*phi1
 *   dC_k/dt        = b_k Sf - lam_k C_k                (Sf = nsf1 phi1 + nsf2 phi2)
 * </pre>
 *
 * <p>Integrated with backward Euler on the diagonal (removal) and fission terms,
 * solved by red-black Gauss-Seidel sweeps, so the stiff prompt scale is stable at
 * a 1 ms step; the precursors are explicit (their scale is seconds). The static
 * critical state is found first (reusing {@link RbmkSolver3D}) and the fission
 * source is scaled so t=0 is exactly critical.
 *
 * <p>This is the replacement for the point-kinetics amplitude: the reactor is a
 * flux field, and the accident is a change of that field.
 */
public final class RbmkTransientSolver {

    private final int ext;
    private final int plane;
    private final int na = RbmkCore.AXIAL_NODES;
    private final boolean[] active;
    private final boolean[] control;
    private final RbmkXsec[] cell;

    /** Neutron flux per group — the state. */
    public final double[] phi1;
    public final double[] phi2;
    /** Six delayed-neutron precursor concentrations, per node. */
    private final double[][] precursor = new double[DelayedNeutrons.GROUPS][];

    private double nsfScale = 1.0;      // scaled so the initial state is critical
    private double lastStepMs;

    /** Rod insertion of the reference critical state (matches the other cores). */
    public static final double NOMINAL_INSERTION = 0.80;

    public RbmkTransientSolver() {
        this.ext = (int) Math.ceil(RbmkGeometry.CHANNEL_REGION_DIAMETER_BLOCKS / 2.0);
        int nx = 2 * ext + 1;
        int nz = 2 * ext + 1;
        this.plane = nz * na;
        int n = nx * nz * na;
        this.active = new boolean[n];
        this.control = new boolean[n];
        this.cell = new RbmkXsec[n];
        for (RbmkLattice.Channel c : RbmkLattice.channels()) {
            for (int a = 0; a < na; a++) {
                int i = idx(c.x(), c.z(), a);
                active[i] = true;
                control[i] = c.control();
            }
        }
        this.phi1 = new double[n];
        this.phi2 = new double[n];
        for (int g = 0; g < DelayedNeutrons.GROUPS; g++) {
            precursor[g] = new double[n];
        }
        setCoreState(0.0, 300.0);
        seedCritical();
    }

    // ------------------------------------------------------------ materials

    /** Set every active fuel node to the given core state. */
    public void setCoreState(double voidFraction, double fuelTempC) {
        RbmkXsec fuel = RbmkXsec.fuelCell(voidFraction, fuelTempC);
        for (int i = 0; i < cell.length; i++) {
            if (active[i] && !control[i]) {
                cell[i] = fuel;
            }
        }
    }

    /** Apply a rod insertion; each axial node gets the rod material there. */
    public void setRodInsertion(double insertion) {
        RbmkGeometry.NodeMaterial[] mats = RbmkGeometry.rodMaterials(insertion);
        RbmkXsec graphite = RbmkXsec.controlCell(RbmkXsec.GRAPHITE);
        RbmkXsec water = RbmkXsec.controlCell(RbmkXsec.WATER);
        RbmkXsec b4c = RbmkXsec.controlCell(RbmkXsec.B4C);
        for (int i = 0; i < cell.length; i++) {
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

    /**
     * Find the static critical flux with {@link RbmkSolver3D} at the current state,
     * copy it in as the initial flux, scale the fission source so it is exactly
     * critical, and set the precursors to equilibrium.
     */
    public void seedCritical() {
        setCoreState(0.0, 300.0);
        setRodInsertion(NOMINAL_INSERTION);
        RbmkSolver3D stat = new RbmkSolver3D();
        stat.setOmega(1.85);
        stat.setInnerSweeps(3);
        stat.setCoreState(0.0, 300.0);
        stat.setRodInsertion(NOMINAL_INSERTION);
        stat.solve(600, 1e-7);
        double k = stat.keff();
        nsfScale = 1.0 / k;
        // copy the shape
        System.arraycopy(stat.phi1, 0, phi1, 0, phi1.length);
        System.arraycopy(stat.phi2, 0, phi2, 0, phi2.length);
        // normalise to unit mean fission over active nodes
        double mean = meanFission();
        if (mean > 0) {
            for (int i = 0; i < phi1.length; i++) {
                phi1[i] /= mean;
                phi2[i] /= mean;
            }
        }
        // precursors at equilibrium: C_k = beta_k Sf / lam_k
        for (int i = 0; i < phi1.length; i++) {
            if (!active[i]) {
                continue;
            }
            double sf = fission(i);
            for (int g = 0; g < DelayedNeutrons.GROUPS; g++) {
                precursor[g][i] = DelayedNeutrons.beta(g) * sf / DelayedNeutrons.LAMBDA[g];
            }
        }
    }

    // ---------------------------------------------------------------- step

    /**
     * Advance the flux by dt seconds with backward Euler on the stiff terms,
     * solved by {@code sweeps} red-black Gauss-Seidel passes; precursors explicit.
     */
    public void step(double dt, int sweeps) {
        long t0 = System.nanoTime();
        double c = dt / DelayedNeutrons.GENERATION_TIME;   // dt / L
        double beta = DelayedNeutrons.BETA_EFFECTIVE;
        int n = phi1.length;
        double[] old1 = phi1.clone();
        double[] old2 = phi2.clone();

        // lagged delayed source and constants per node are recomputed in the sweep
        for (int s = 0; s < sweeps; s++) {
            boolean red = (s & 1) == 0;
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
                        RbmkXsec xs = cell[i];
                        int nb = neighbours(x, z, a);

                        // --- group 1 ---
                        double delayed = 0;
                        for (int g = 0; g < DelayedNeutrons.GROUPS; g++) {
                            delayed += DelayedNeutrons.LAMBDA[g] * precursor[g][i];
                        }
                        double sf1 = xs.nsf1() * phi1[i] + xs.nsf2() * phi2[i];
                        double nsf1s = nsfScale * xs.nsf1();
                        double nsf2s = nsfScale * xs.nsf2();
                        double a1 = 1.0 + c * (xs.sr1() - (1 - beta) * nsf1s) + c * xs.d1() * nb;
                        double b1 = c * xs.d1();
                        double f2 = c * ((1 - beta) * nsf2s + xs.ss21());
                        double rhs1 = old1[i] + c * delayed + f2 * phi2[i];
                        double t1 = (rhs1 + b1 * sum(phi1, x, z, a)) / a1;
                        phi1[i] = t1;

                        // --- group 2 ---
                        double a2 = 1.0 + c * (xs.sa2() + xs.ss21()) + c * xs.d2() * nb;
                        double b2 = c * xs.d2();
                        double f1 = c * xs.ss12();
                        double t2 = (old2[i] + f1 * phi1[i] + b2 * sum(phi2, x, z, a)) / a2;
                        phi2[i] = t2;
                    }
                }
            }
        }

        // precursors: explicit over the step, using the updated fission source
        for (int i = 0; i < n; i++) {
            if (!active[i]) {
                continue;
            }
            double sf = fission(i);
            for (int g = 0; g < DelayedNeutrons.GROUPS; g++) {
                double lam = DelayedNeutrons.LAMBDA[g];
                double ci = precursor[g][i];
                precursor[g][i] = ci + dt * (DelayedNeutrons.beta(g) * sf - lam * ci);
            }
        }
        lastStepMs = (System.nanoTime() - t0) / 1e6;
    }

    // --------------------------------------------------------------- reads

    /** Mean fission source over the active nodes — the reactor power (arbitrary units). */
    public double power() {
        return meanFission();
    }

    /** The largest local fission rate — where the core is hottest / nearest runaway. */
    public double peakFission() {
        double p = 0;
        for (int i = 0; i < phi1.length; i++) {
            if (active[i]) {
                p = Math.max(p, fission(i));
            }
        }
        return p;
    }

    /** Milliseconds the last {@link #step} took. */
    public double lastStepMs() {
        return lastStepMs;
    }

    private double meanFission() {
        double s = 0;
        int n = 0;
        for (int i = 0; i < phi1.length; i++) {
            if (active[i]) {
                s += fission(i);
                n++;
            }
        }
        return n == 0 ? 0 : s / n;
    }

    private double fission(int i) {
        RbmkXsec xs = cell[i];
        return nsfScale * (xs.nsf1() * phi1[i] + xs.nsf2() * phi2[i]);
    }

    // -------------------------------------------------------------- stencil

    private double sum(double[] phi, int x, int z, int a) {
        double s = 0;
        if (isActive(x + 1, z, a)) s += phi[idx(x + 1, z, a)];
        if (isActive(x - 1, z, a)) s += phi[idx(x - 1, z, a)];
        if (isActive(x, z + 1, a)) s += phi[idx(x, z + 1, a)];
        if (isActive(x, z - 1, a)) s += phi[idx(x, z - 1, a)];
        if (a + 1 < na && isActive(x, z, a + 1)) s += phi[idx(x, z, a + 1)];
        if (a - 1 >= 0 && isActive(x, z, a - 1)) s += phi[idx(x, z, a - 1)];
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
        return x >= -ext && x <= ext && z >= -ext && z <= ext && a >= 0 && a < na
            && active[idx(x, z, a)];
    }

    private int idx(int x, int z, int a) {
        return (x + ext) * plane + (z + ext) * na + a;
    }
}
