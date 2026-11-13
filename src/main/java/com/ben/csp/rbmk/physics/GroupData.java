package com.ben.csp.rbmk.physics;

import java.util.Arrays;

/**
 * Group-general homogenised neutron cross sections for one node: G energy groups
 * with the full scattering matrix between them, in cm^-1, plus the fission
 * spectrum.
 *
 * <p>This is {@link RbmkXsec} widened from two groups. Group 1 is the fastest,
 * group G the most thermal. The steady-state operator is
 * <pre>
 *   -D_g lap(phi_g) + [Sa_g + sum_{j!=g} Ss[g][j]] phi_g
 *       = chi_g (1/k) sum_g' nsf_g' phi_g'  +  sum_{j!=g} Ss[j][g] phi_j
 * </pre>
 * i.e. {@code Ss[from][to]} scatters from {@code from} into {@code to} (rows are
 * the source group). In two groups this reduces exactly to the fields of
 * {@link RbmkXsec}: g=1 fast ({@code d1, sr1, nsf1}), g=2 thermal
 * ({@code d2, sa2, nsf2}), {@code Ss[1][2]} the down-scatter {@code ss12},
 * {@code Ss[2][1]} the up-scatter {@code ss21}, and chi = {1, 0} (fission born
 * fast) — which is what {@link RbmkSolver3D} assumes.
 *
 * <p>The number of groups is not fixed: the solver reads {@link #groups()}. A
 * 2-group dataset is the G=2 case, so the same engine runs both.
 */
public final class GroupData {

    private final int g;
    private final double[] d;      // diffusion coefficient, cm
    private final double[] sa;     // absorption, cm^-1
    private final double[] nsf;    // nu*Sigma_f, cm^-1
    private final double[][] ss;   // scattering matrix, ss[from][to], cm^-1
    private final double[] chi;    // fission spectrum, sums to 1

    public GroupData(double[] d, double[] sa, double[] nsf, double[][] ss, double[] chi) {
        this.g = d.length;
        if (sa.length != g || nsf.length != g || ss.length != g || chi.length != g) {
            throw new IllegalArgumentException("group count mismatch");
        }
        for (double[] row : ss) {
            if (row.length != g) {
                throw new IllegalArgumentException("scattering matrix is not G x G");
            }
        }
        this.d = d.clone();
        this.sa = sa.clone();
        this.nsf = nsf.clone();
        this.chi = chi.clone();
        this.ss = new double[g][g];
        for (int i = 0; i < g; i++) {
            this.ss[i] = ss[i].clone();
        }
    }

    public int groups() {
        return g;
    }

    public double d(int i) {
        return d[i];
    }

    public double sa(int i) {
        return sa[i];
    }

    public double nsf(int i) {
        return nsf[i];
    }

    public double chi(int i) {
        return chi[i];
    }

    /** Scattering from group {@code from} into group {@code to}. */
    public double scatter(int from, int to) {
        return ss[from][to];
    }

    /** Removal from group i: absorption plus all out-scatter. */
    public double removal(int i) {
        double r = sa[i];
        for (int j = 0; j < g; j++) {
            if (j != i) {
                r += ss[i][j];
            }
        }
        return r;
    }

    /** In-scatter into group i from all other groups at one node. */
    public double inScatter(int i, double[][] phi, int node) {
        double s = 0;
        for (int j = 0; j < g; j++) {
            if (j != i) {
                s += ss[j][i] * phi[j][node];
            }
        }
        return s;
    }

    /** Build from a two-group {@link RbmkXsec} (the G=2 case, chi = {1, 0}). */
    public static GroupData of(RbmkXsec xs) {
        double[] d = {xs.d1(), xs.d2()};
        double[] sa = {xs.sr1() - xs.ss12(), xs.sa2()};
        double[] nsf = {xs.nsf1(), xs.nsf2()};
        double[][] ss = {
            {0.0, xs.ss12()},
            {xs.ss21(), 0.0},
        };
        return new GroupData(d, sa, nsf, ss, new double[] {1.0, 0.0});
    }

    /**
     * Widen a two-group set to {@code target} groups by block-repeating each group,
     * such that the target-group system is EXACTLY equivalent to the two-group one
     * (uniform sub-flux within each block). Used only to exercise and test a G&gt;2
     * engine before real fine-group data exists — it is NOT physics, and every such
     * set is marked a placeholder.
     *
     * <p>Equivalence: with H sub-groups per block and sub-flux f = PHI/H, a block
     * sums to the two-group equation when each sub-group carries the block's
     * absorption and nu-Sigma_f (so sum_i nsf_i f_i = nsf PHI), each sub-group
     * scatters out the full block out-scatter, and the inter-block transfer is
     * split Ss/H so a sub-group receives the block total.
     */
    public static GroupData repeat(RbmkXsec xs, int target) {
        if (target % 2 != 0) {
            throw new IllegalArgumentException("target must be even");
        }
        int h = target / 2;
        double[] d = new double[target];
        double[] sa = new double[target];
        double[] nsf = new double[target];
        double[] chi = new double[target];
        double[][] ss = new double[target][target];
        for (int i = 0; i < target; i++) {
            boolean fast = i < h;
            d[i] = fast ? xs.d1() : xs.d2();
            sa[i] = fast ? xs.sr1() - xs.ss12() : xs.sa2();
            nsf[i] = fast ? xs.nsf1() : xs.nsf2();
            chi[i] = fast ? 1.0 / h : 0.0;
            for (int j = 0; j < target; j++) {
                boolean toFast = j < h;
                if (fast == toFast) {
                    ss[i][j] = 0.0;            // within a block
                } else if (fast) {
                    ss[i][j] = xs.ss12() / h;  // fast block -> thermal block
                } else {
                    ss[i][j] = xs.ss21() / h;  // thermal block -> fast block
                }
            }
        }
        return new GroupData(d, sa, nsf, ss, chi);
    }

    @Override
    public String toString() {
        return "GroupData[G=" + g + ", sa=" + Arrays.toString(sa)
            + ", nsf=" + Arrays.toString(nsf) + "]";
    }
}
