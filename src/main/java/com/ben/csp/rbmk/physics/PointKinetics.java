package com.ben.csp.rbmk.physics;

import java.util.ArrayList;
import java.util.List;

/**
 * Point reactor kinetics: the core calculation a Soviet operator ran (by hand, or
 * on the SKALA computer) to answer "if I move a rod, how fast does the power
 * change?". It is the same set of equations here as on a real RBMK — six delayed
 * groups plus the prompt term — so the tests that can be run on a real reactor can
 * be run on this model.
 *
 * <p>   dP/dt = (rho - beta)/Lambda * P + sum_i lambda_i * C_i
 * <p>   dC_i/dt = beta_i/Lambda * P - lambda_i * C_i
 *
 * <p>Everything is normalised: P is power relative to start (P(0) = 1), C_i are
 * scaled so the core is at equilibrium at the start (dC_i/dt = 0 at t=0).
 */
public final class PointKinetics {

    private double rho;         // reactivity, dimensionless (dk/k)
    private double power = 1.0;
    private final double[] precursor = new double[DelayedNeutrons.GROUPS];

    public PointKinetics() {
        equilibrium();
    }

    /** Set the precursors so the core is critical and steady at t=0. */
    public void equilibrium() {
        for (int i = 0; i < DelayedNeutrons.GROUPS; i++) {
            precursor[i] = DelayedNeutrons.beta(i) * power
                / (DelayedNeutrons.LAMBDA[i] * DelayedNeutrons.GENERATION_TIME);
        }
    }

    public double reactivity() {
        return rho;
    }

    /** Set reactivity in dk/k, or in units of beta (dollars) via {@link #setDollars}. */
    public void setReactivity(double rho) {
        this.rho = rho;
    }

    public void setDollars(double dollars) {
        this.rho = dollars * DelayedNeutrons.BETA_EFFECTIVE;
    }

    public double power() {
        return power;
    }

    public double[] precursors() {
        return precursor;
    }

    /**
     * Advance by one step with a semi-implicit (backward-Euler on the prompt term)
     * scheme, which is stable even when rho exceeds beta — the region that matters,
     * because that is where the power runs away on the prompt neutrons.
     */
    public void step(double dt) {
        double lambda = DelayedNeutrons.GENERATION_TIME;
        double beta = DelayedNeutrons.BETA_EFFECTIVE;

        // Prompt term is implicit: P_new = P + dt*((rho-beta)/Lambda * P_new + S)
        double source = 0;
        for (int i = 0; i < DelayedNeutrons.GROUPS; i++) {
            source += DelayedNeutrons.LAMBDA[i] * precursor[i];
        }
        double a = (rho - beta) / lambda;
        double pNew = (power + dt * source) / (1.0 - dt * a);

        // Precursors, explicit, using the new power (also unconditionally fine:
        // the precursor equations are stiff only in the decay, handled below).
        for (int i = 0; i < DelayedNeutrons.GROUPS; i++) {
            double lam = DelayedNeutrons.LAMBDA[i];
            double c0 = precursor[i];
            double cInf = DelayedNeutrons.beta(i) / (lam * lambda) * pNew;
            double decay = Math.exp(-lam * dt);
            precursor[i] = cInf + (c0 - cInf) * decay;
        }
        power = pNew;
    }

    public void run(double dt, double seconds, List<Double> powerLog, List<Double> timeLog) {
        int n = (int) Math.round(seconds / dt);
        for (int k = 0; k < n; k++) {
            step(dt);
            if (powerLog != null) {
                powerLog.add(power);
                timeLog.add((k + 1) * dt);
            }
        }
    }

    /**
     * Inhour equation: the stable reactor period T (s) for a given reactivity, from
     * the roots of  rho = Lambda/T + sum_i beta_i / (1 + lambda_i T).
     * Solved by bisection — the same relation plotted on every reactor's start-up
     * chart.
     */
    public static double period(double rho) {
        // f(T) = Lambda/T + sum beta_i/(1+lambda_i T) - rho, decreasing in T>0.
        double lo = 1e-6;
        double hi = 1e6;
        for (int i = 0; i < 200; i++) {
            double mid = Math.sqrt(lo * hi); // geometric mid: spans many decades
            double f = inhour(mid, rho);
            if (f > 0) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return Math.sqrt(lo * hi);
    }

    private static double inhour(double T, double rho) {
        double v = DelayedNeutrons.GENERATION_TIME / T;
        for (int i = 0; i < DelayedNeutrons.GROUPS; i++) {
            v += DelayedNeutrons.beta(i) / (1.0 + DelayedNeutrons.LAMBDA[i] * T);
        }
        return v - rho;
    }

    /** Prompt-neutron lifetime approximation of the period, for small T. */
    public static double asymptoticPeriod(double rho) {
        return DelayedNeutrons.GENERATION_TIME / rho;
    }
}
