package com.ben.csp.rbmk.physics;

/**
 * The accident sequence, run on the coupled core: the reason all this physics
 * exists. It reproduces, in order:
 * <ol>
 *   <li>a steady, critical core at power;</li>
 *   <li>rod withdrawal raising the power (a positive reactor period);</li>
 *   <li>a power reduction, after which xenon builds into a pit;</li>
 *   <li>the operator withdrawing rods to fight the xenon;</li>
 *   <li>AZ-5: a scram that, because of the graphite displacers, first RAISES
 *       reactivity — the positive scram — before it shuts the core down.</li>
 * </ol>
 */
public final class AccidentSequence {

    private static int checks = 0;

    public static void main(String[] args) {
        double dt = 0.05;

        // ---- 1. steady, critical core ----
        ReactorCore core = new ReactorCore();
        for (int k = 0; k < 5; k++) {
            System.out.printf("  t=%3.0fs  P=%.4f  rho=%+.6f  void=%.4f  Tf=%.1f  Xe=%.4f%n",
                k * 24.0, core.power(), core.reactivity(), core.voidFraction(),
                core.fuelTempC(), core.xenonRatio());
            run(core, dt, 24);
        }
        check(Math.abs(core.power() - 1.0) < 0.05,
            "the core holds steady at nominal power (" + round(core.power()) + ")");
        check(Math.abs(core.reactivity()) < 1e-4,
            "the nominal core is critical, rho ~ 0 (" + round(core.reactivity()) + ")");

        // ---- 2. withdraw rods: power rises on a positive period ----
        core.setRodTarget(0.74);
        run(core, dt, 30);
        System.out.printf("  after withdrawal: P=%.4f rho=%+.6f%n", core.power(), core.reactivity());
        check(core.reactivity() > 0,
            "withdrawing rods makes the core super-critical (rho " + round(core.reactivity()) + ")");
        check(core.power() > 1.0,
            "withdrawing rods raises the power (" + round(core.power()) + ")");
        double period = periodOf(core);
        check(period > 0, "the power is rising on a positive period (~" + round(period) + " s)");

        // ---- 3. reduce power ----
        core.setRodTarget(0.90);
        run(core, dt, 180);
        System.out.printf("  after power reduction: P=%.4f rho=%+.6f ins=%.3f%n",
            core.power(), core.reactivity(), core.insertion());
        double lowPower = core.power();
        check(lowPower < 0.9, "the power comes down after the rods go in (" + round(lowPower) + ")");
        check(lowPower > 0.01, "the power stays positive, not shut down (" + round(lowPower) + ")");

        // ---- 4. the xenon pit: the core runs at low power as the poison builds ----
        core.setRodTarget(0.82);
        run(core, dt, 6 * 3600);
        System.out.printf("  after 6 h: P=%.4f Xe=%.4fx rho=%+.6f%n",
            core.power(), core.xenonRatio(), core.reactivity());
        check(core.xenonRatio() > 1.05,
            "xenon builds into a pit after the power cut (Xe " + round(core.xenonRatio()) + "x)");
        check(core.reactivity() < 0,
            "the xenon poison drives the core sub-critical (rho " + round(core.reactivity()) + ")");

        // ---- 4b. the operator withdraws the rods to the ORM limit to fight the
        // xenon. This is the fatal state: almost all rods out, so the whole core
        // sits on a tiny operating margin (the "ORM of 8"). ----
        core.setRodTarget(0.10);
        run(core, dt, 90);
        System.out.printf("  rods withdrawn to the ORM limit: P=%.4f rho=%+.6f ins=%.3f%n",
            core.power(), core.reactivity(), core.insertion());
        check(core.insertion() < 0.2,
            "the rods are withdrawn to the top - a low operating margin (ins "
                + round(core.insertion()) + ")");
        check(core.reactivity() < 0.002,
            "the core is on a small margin, near critical (rho "
                + round(core.reactivity()) + ")");

        // ---- 5. AZ-5: the positive scram ----
        // It is a property of the rod geometry: with the accident's bottom-peaked
        // axial flux the initial differential worth is positive.
        double[] dw = RodWorth.bottomPeaked().differentialCurve(40);
        check(dw[0] > 0,
            "the bottom-peaked rod worth starts POSITIVE - the positive scram (dW0 "
                + round(dw[0]) + ")");

        double rhoBefore = core.reactivity();
        double powerBefore = core.power();
        core.setRodTarget(1.0);
        double maxRho = rhoBefore;
        double maxPower = powerBefore;
        int steps = (int) (25 / dt);
        for (int k = 0; k < steps; k++) {
            core.step(dt);
            if (core.reactivity() > maxRho) {
                maxRho = core.reactivity();
            }
            if (core.power() > maxPower) {
                maxPower = core.power();
            }
        }
        if (powerBefore > 0.01) {
            check(maxRho > rhoBefore,
                "AZ-5 first RAISES reactivity - the positive scram (rho "
                    + round(rhoBefore) + " -> " + round(maxRho) + ")");
            check(maxPower > powerBefore,
                "the power rises during the first seconds of the scram ("
                    + round(powerBefore) + " -> " + round(maxPower) + ")");
            check(core.power() < maxPower,
                "then the absorber takes over and the power falls ("
                    + round(maxPower) + " -> " + round(core.power()) + ")");
        }

        System.out.println();
        System.out.println("AZ-5 sequence:");
        System.out.printf("  rho before scram      : %+.5f%n", rhoBefore);
        System.out.printf("  peak rho (positive)   : %+.5f  (+%.5f from the displacers)%n",
            maxRho, maxRho - rhoBefore);
        System.out.printf("  power before scram    : %.3f%n", powerBefore);
        System.out.printf("  peak power during AZ-5: %.3f%n", maxPower);
        System.out.printf("  power at end          : %.3f%n", core.power());

        System.out.println();
        System.out.println("AccidentSequence: " + checks + " checks passed.");
    }

    private static void run(ReactorCore core, double dt, double seconds) {
        int n = (int) (seconds / dt);
        for (int k = 0; k < n; k++) {
            core.step(dt);
        }
    }

    /** Approximate instantaneous period from two samples a second apart. */
    private static double periodOf(ReactorCore core) {
        double p1 = core.power();
        run(core, 0.05, 1.0);
        double p2 = core.power();
        if (p2 <= p1) {
            return 0;
        }
        return 1.0 / Math.log(p2 / p1);
    }

    private static String round(double v) {
        return String.format("%.5f", v);
    }

    private static void check(boolean cond, String what) {
        checks++;
        if (!cond) {
            throw new AssertionError(what);
        }
    }
}
