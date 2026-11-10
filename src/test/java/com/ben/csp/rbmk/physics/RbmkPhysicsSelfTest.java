package com.ben.csp.rbmk.physics;

import java.util.ArrayList;
import java.util.List;

/**
 * Validation of the reactor physics against the classic checks that are run on a
 * real reactor — not a smoke test. If these pass, the same calculations an
 * operator or a SKALA programme ran can be run here.
 *
 * <p>Checks: delayed-neutron data consistency; steady state at zero reactivity;
 * the prompt jump; agreement between the inhour period and the simulated period
 * (two independent routes to the same number); prompt-critical runaway; and the
 * xenon pit after a power reduction.
 */
public final class RbmkPhysicsSelfTest {

    private static int checks = 0;

    public static void main(String[] args) {
        // --- data consistency ---
        check(Math.abs(DelayedNeutrons.fractionSum() - 1.0) < 1e-9,
            "delayed-neutron group fractions sum to 1");
        eq(DelayedNeutrons.GROUPS, DelayedNeutrons.LAMBDA.length, "six decay constants");
        eq(DelayedNeutrons.GROUPS, DelayedNeutrons.BETA_FRACTION.length, "six abundances");

        // --- steady state: zero reactivity, power holds ---
        PointKinetics pk = new PointKinetics();
        pk.run(0.01, 50.0, null, null);
        check(Math.abs(pk.power() - 1.0) < 1e-3,
            "at rho=0 the power holds steady (" + pk.power() + ")");

        // --- prompt jump: inserting rho<beta gives an immediate rise of 1/(1-rho/beta) ---
        // The jump settles over Lambda/(beta-rho) ~ 0.13 s; integrate 0.5 s so the
        // subsequent delayed rise is still negligible.
        double rho = 0.001; // < beta = 0.0065
        PointKinetics pj = new PointKinetics();
        pj.setReactivity(rho);
        pj.run(0.002, 0.5, null, null);
        double expectedJump = 1.0 / (1.0 - rho / DelayedNeutrons.BETA_EFFECTIVE);
        check(Math.abs(pj.power() - expectedJump) / expectedJump < 0.05,
            "prompt jump matches 1/(1-rho/beta): expected ~" + round(expectedJump)
                + " got " + round(pj.power()));

        // --- inhour period vs simulated period (two independent methods) ---
        double rho2 = 0.0005;
        double inhour = PointKinetics.period(rho2);
        PointKinetics sp = new PointKinetics();
        sp.setReactivity(rho2);
        double dt = 0.02;
        // let the prompt jump settle, then measure over a long window
        sp.run(dt, 50.0, null, null);
        double p1 = sp.power();
        sp.run(dt, 200.0, null, null);
        double p2 = sp.power();
        double periodSim = 200.0 / Math.log(p2 / p1);
        check(inhour > 0 && periodSim > 0, "positive period for positive reactivity");
        check(relErr(inhour, periodSim) < 0.15,
            "inhour period agrees with simulated period: inhour " + round(inhour)
                + " s vs simulated " + round(periodSim) + " s");
        check(inhour > 20 && inhour < 200, "period at rho=0.0005 is tens of seconds ("
            + round(inhour) + " s)");

        // --- period falls as reactivity rises ---
        check(PointKinetics.period(0.001) < PointKinetics.period(0.0005),
            "the period shortens as reactivity increases");

        // --- prompt critical: rho > beta runs away on the prompt neutrons ---
        PointKinetics pc = new PointKinetics();
        pc.setReactivity(DelayedNeutrons.BETA_EFFECTIVE + 0.001); // above one dollar
        double before = pc.power();
        pc.run(0.01, 1.0, null, null); // one second
        check(pc.power() > before * 5,
            "above prompt critical the power runs away within a second (x"
                + round(pc.power() / before) + ")");

        // --- xenon pit: drop power, xenon overshoots then decays ---
        Xenon xe = new Xenon(1.0);       // equilibrium at full power
        double xeEq = xe.xenon();
        double worthEq = xe.worth();
        check(worthEq < 0 && Math.abs(worthEq + 0.03) < 1e-9,
            "xenon worth is -3% at the full-power equilibrium");
        xe.setFlux(0.1);                 // power reduced to 10%
        List<Double> trace = new ArrayList<>();
        double h = 60.0;                 // 1-minute steps
        double peak = 0;
        double peakHour = 0;
        for (int i = 0; i < 24 * 60; i++) { // 24 hours
            xe.step(h);
            double x = xe.xenon();
            trace.add(x);
            if (x > peak) {
                peak = x;
                peakHour = (i + 1) * h / 3600.0;
            }
        }
        double ratio = peak / xeEq;
        check(ratio > 1.3 && ratio < 2.2,
            "xenon overshoots after a power cut (the xenon pit): peak "
                + round(ratio * 100) + "% of the full-power value");
        check(peakHour > 4 && peakHour < 12,
            "the xenon peak comes about 7-8 h after the power cut (peak at "
                + round(peakHour) + " h)");
        check(xe.worth() < worthEq, "xenon worth is more negative in the pit");
        double at24h = trace.get(trace.size() - 1);
        check(at24h < peak, "the xenon peak then decays away");

        System.out.println("RbmkPhysicsSelfTest: " + checks + " checks passed.");
    }

    private static double relErr(double a, double b) {
        return Math.abs(a - b) / Math.max(Math.abs(a), 1e-12);
    }

    private static String round(double v) {
        return String.format("%.3f", v);
    }

    private static void check(boolean cond, String what) {
        checks++;
        if (!cond) {
            throw new AssertionError(what);
        }
    }

    private static void eq(int expected, int actual, String what) {
        check(expected == actual, what + ": expected <" + expected + "> but was <" + actual + ">");
    }
}
