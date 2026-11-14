package com.ben.csp.rbmk.physics;

import com.ben.csp.rbmk.RbmkGeometry;

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
 * where the rod's axial material (B4C / telescope / graphite displacer) comes from
 * {@link RbmkGeometry#rodMaterials}. Replacing water (a weak absorber) with graphite
 * removes absorption, so that node contributes <em>positive</em> reactivity — the
 * mechanism that made AZ-5 briefly raise the power.
 */
public final class RodWorth {

    private static final int NODES = RbmkGeometry.CORE_HEIGHT_CELLS;
    private static final double CORE_H = RbmkGeometry.CORE_HEIGHT_M;

    private static final double SA_WATER = 0.020;
    private static final double SA_GRAPHITE = 0.0005;
    private static final double SA_B4C = 0.450;
    /**
     * Scale to dk/k: the fractional worth of one full B4C column. A reference
     * value ({@link PhysicsProvenance} {@code rodworth.scale}, {@code to_verify});
     * replace with a measured/design rod-worth curve before freezing.
     */
    private static final double SCALE = 0.08;

    private final double[] flux2;

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

    /** Reactivity (dk/k) contributed by a rod at the given insertion, 0..1. */
    public double worth(double insertion) {
        RbmkGeometry.NodeMaterial[] mat = RbmkGeometry.rodMaterials(insertion);
        double num = 0;
        double den = 0;
        for (int z = 0; z < NODES; z++) {
            num += -(sa(mat[z]) - SA_WATER) * flux2[z];
            den += flux2[z];
        }
        return SCALE * num / den;
    }

    /** Integral worth curve: worth at each insertion step from 0 to 1. */
    public double[] integralCurve(int steps) {
        double[] out = new double[steps + 1];
        for (int i = 0; i <= steps; i++) {
            out[i] = worth((double) i / steps);
        }
        return out;
    }

    /** Differential worth dW/dx at each insertion step (per unit insertion). */
    public double[] differentialCurve(int steps) {
        double[] w = integralCurve(steps);
        double[] out = new double[steps];
        for (int i = 0; i < steps; i++) {
            out[i] = (w[i + 1] - w[i]) * steps;
        }
        return out;
    }
}
