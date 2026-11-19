package com.ben.csp.rbmk.physics;

import com.ben.csp.rbmk.RbmkGeometry;
import com.ben.csp.rbmk.RodType;

/**
 * Control-rod worth: the reactivity a rod adds as it is driven in, split into the
 * integral worth (total, over the travel) and the differential worth (per unit of
 * travel). These are the curves in every rod-worth calculation, and the RBMK's are
 * special — because of the graphite displacer, the differential worth is not simply
 * negative: at the start of insertion it can be <em>positive</em>.
 *
 * <p>Method: first-order perturbation theory over the core height. A node's
 * contribution is proportional to the change in absorption it introduces times the
 * square of the axial flux there:
 * <pre>
 *   rho(x) = - sum_z [ Sa_rod(z,x) - Sa_water ] * phi(z)^2 / sum_z phi(z)^2
 * </pre>
 * Replacing water (a weak absorber) with graphite removes absorption, so that node
 * contributes <em>positive</em> reactivity — the mechanism that made AZ-5 briefly
 * raise the power.
 *
 * <p><b>Calibration.</b> The absolute scale is fixed by the one rod-worth figure the
 * unit 4 panel states outright: the 24 АЗ rods together are worth -2 beta_eff
 * ({@code rod.worth_group}). One fully inserted АЗ rod is therefore worth
 * {@code 2 * beta_eff / 24}, and every other type follows from the same nodal
 * integral scaled to its own absorber and displacer lengths ({@code rod.lengths},
 * {@code rod.displacer_lengths}). This replaces the previous hand-set
 * {@code SCALE = 0.08}, which was flagged {@code to_verify} and which reproduced
 * neither the margin nor the group worth.
 */
public final class RodWorth {

    private static final int NODES = RbmkGeometry.CORE_HEIGHT_CELLS;
    private static final double CORE_H = RbmkGeometry.CORE_HEIGHT_M;

    private static final double SA_WATER = 0.020;
    private static final double SA_GRAPHITE = 0.0005;
    private static final double SA_B4C = 0.450;

    /** beta_eff, the unit the panel's rod-worth figures are quoted in. */
    private static final double BETA_EFF = 0.0065;

    private final double[] flux2;

    /** Worth of one fully inserted АЗ rod, dk/k — the calibration anchor. */
    private final double azRodWorth;

    private final double fullColumn;

    /** Centre-peaked (fundamental) axial flux: a rod at nominal power. */
    public RodWorth() {
        this(centrePeakedFlux());
    }

    /** With an explicit axial flux-squared shape (length {@link #nodes()}). */
    public RodWorth(double[] axialFlux2) {
        if (axialFlux2.length != NODES) {
            throw new IllegalArgumentException("flux must have " + NODES + " nodes");
        }
        this.flux2 = axialFlux2.clone();
        this.azRodWorth = RodType.AZ.groupWorthBeta * BETA_EFF / RodType.AZ.count;
        this.fullColumn = fullColumnIntegral();
    }

    public static int nodes() {
        return NODES;
    }

    private static double[] centrePeakedFlux() {
        double hEff = CORE_H + 1.0;
        double[] f = new double[NODES];
        for (int z = 0; z < NODES; z++) {
            double phi = Math.sin(Math.PI * (z + 0.5) * (CORE_H / NODES) / hEff);
            f[z] = phi * phi;
        }
        return f;
    }

    /**
     * Bottom-peaked axial flux — the accident state after the power reduction,
     * with the upper part of the core poisoned (xenon and the withdrawn rods'
     * displacers) so the flux is concentrated toward the bottom.
     */
    public static RodWorth bottomPeaked() {
        double[] f = new double[NODES];
        for (int z = 0; z < NODES; z++) {
            f[z] = Math.exp(-6.0 * (z + 0.5) / NODES);
        }
        return new RodWorth(f);
    }

    /** Absorption cross section of a node's material, cm^-1. */
    private static double sa(RbmkGeometry.NodeMaterial m) {
        return switch (m) {
            case WATER -> SA_WATER;
            case GRAPHITE -> SA_GRAPHITE;
            case B4C -> SA_B4C;
        };
    }

    /** The water-filled gap between a rod's displacer and its absorber, m. */
    private static final double TELESCOPE_M = RbmkGeometry.ROD_TELESCOPE_M;

    /**
     * The axial material of a rod of this type at this insertion.
     *
     * <p>The rod is a stack: graphite displacer at the foot, a water-filled
     * telescope above it, then the B4C absorber on top. Withdrawn, the absorber
     * sits above the core and the displacer is in the upper-middle of it; driving
     * the rod in moves the whole stack down, so the displacer sweeps down through
     * the core <em>before</em> the absorber arrives. That is the positive-scram
     * geometry. АЗ rods have no displacer, so they are absorber alone.
     *
     * <p>УСП rods are inverted — driven from below, absorber rising from the core
     * bottom with the displacer above it.
     */
    public static RbmkGeometry.NodeMaterial[] rodMaterials(RodType t, double insertion) {
        double d = clamp01(insertion) * CORE_H;
        double nodeH = CORE_H / NODES;
        double gap = t.displacerM > 0 ? TELESCOPE_M : 0.0;
        double absBottom;
        double absTop;
        double dispBottom;
        double dispTop;
        if (t.fromBelow) {
            absBottom = d - t.absorberM;
            absTop = d;
            dispBottom = d + gap;
            dispTop = dispBottom + t.displacerM;
        } else {
            double stackBottom = CORE_H - d - t.displacerM - gap;
            dispBottom = stackBottom;
            dispTop = stackBottom + t.displacerM;
            absBottom = dispTop + gap;
            absTop = absBottom + t.absorberM;
        }
        RbmkGeometry.NodeMaterial[] out = new RbmkGeometry.NodeMaterial[NODES];
        for (int z = 0; z < NODES; z++) {
            double h = (z + 0.5) * nodeH;
            if (h >= absBottom && h <= absTop) {
                out[z] = RbmkGeometry.NodeMaterial.B4C;
            } else if (h >= dispBottom && h <= dispTop) {
                out[z] = RbmkGeometry.NodeMaterial.GRAPHITE;
            } else {
                out[z] = RbmkGeometry.NodeMaterial.WATER;
            }
        }
        return out;
    }

    /** The nodal integral for a material vector, in units of a full B4C column. */
    private double integral(RbmkGeometry.NodeMaterial[] mat) {
        double num = 0;
        double den = 0;
        for (int z = 0; z < NODES; z++) {
            num += -(sa(mat[z]) - SA_WATER) * flux2[z];
            den += flux2[z];
        }
        return num / den;
    }

    /** Nodal integral of a full-core B4C column — the normalising denominator. */
    private double fullColumnIntegral() {
        RbmkGeometry.NodeMaterial[] full = new RbmkGeometry.NodeMaterial[NODES];
        java.util.Arrays.fill(full, RbmkGeometry.NodeMaterial.B4C);
        return integral(full);
    }

    /** Reactivity (dk/k) contributed by a rod of this type at this insertion, 0..1. */
    public double worth(RodType t, double insertion) {
        return azRodWorth * integral(rodMaterials(t, insertion)) / fullColumn;
    }

    /**
     * Reactivity (dk/k) of a manual rod at this insertion. Retained for the
     * axial-shape diagnostics; identical to {@code worth(RodType.RR, insertion)}.
     */
    public double worth(double insertion) {
        return worth(RodType.RR, insertion);
    }

    /** Integral worth curve for one rod type: worth at each insertion step, 0..1. */
    public double[] integralCurve(RodType t, int steps) {
        double[] out = new double[steps + 1];
        for (int i = 0; i <= steps; i++) {
            out[i] = worth(t, (double) i / steps);
        }
        return out;
    }

    /** Differential worth dW/dx at each insertion step (per unit insertion). */
    public double[] differentialCurve(RodType t, int steps) {
        double[] w = integralCurve(t, steps);
        double[] out = new double[steps];
        for (int i = 0; i < steps; i++) {
            out[i] = (w[i + 1] - w[i]) * steps;
        }
        return out;
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }
}
