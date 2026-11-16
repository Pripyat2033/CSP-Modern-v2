package com.ben.csp.rbmk.physics;

/**
 * The accident sequence, run on the one real core ({@link UnifiedReactorCore}).
 *
 * <p><b>There is no accident mode.</b> This test never sets a flag, a config or a
 * special state: it only moves the rods and steps the physics, exactly as an
 * operator would. The runaway is not a mode the reactor enters — it is a place the
 * same core can be driven to by a sequence of ordinary operating decisions. That is
 * the point: at Ignalina in 1983 the operators found the power RISING when they
 * inserted the rods, under normal operation. These cores were latent positive-
 * feedback devices, not reactors with an "accident button".
 *
 * <p>The sequence it reproduces, in order:
 * <ol>
 *   <li>a steady, critical core at power;</li>
 *   <li>rod withdrawal raising the power (a positive reactor period);</li>
 *   <li>a power reduction, after which xenon builds into a pit;</li>
 *   <li>the operator withdrawing rods to fight the xenon (the low-margin state);</li>
 *   <li>AZ-5: a scram that, because of the graphite displacers, first RAISES
 *       reactivity — the positive scram — before it shuts the core down.</li>
 * </ol>
 *
 * <p>Every number asserted here is MEASURED from the core, not assumed. The rod
 * worth is the live core's own (derived from the rod geometry); the fitted
 * {@code RodWorth.bottomPeaked()} curve is not consulted.
 */
public final class AccidentSequence {

    private static int checks = 0;
    private static int failures = 0;

    /**
     * Numerical step for this long sequence. The integration is stable and accurate
     * far coarser than the 5 ms production step (backward Euler on the stiff removal,
     * an exact exponential on the xenon chain), so the 6 h xenon phase is tractable
     * here. Production runs at {@link UnifiedReactorCore#DT}.
     */
    private static final double DT = 0.25;

    public static void main(String[] args) {
        // ---- 1. steady, critical core ----
        UnifiedReactorCore core = new UnifiedReactorCore();
        core.setPhysicsDt(DT);
        run(core, DT, 120);
        System.out.printf("  1 nominal   : P=%.4f rho=%+.6f ins=%.3f%n",
            core.power(), core.reactivity(), core.insertion());
        check(Math.abs(core.power() - 1.0) < 0.05,
            "the core holds steady at nominal power (" + round(core.power()) + ")");
        check(Math.abs(core.reactivity()) < 1e-3,
            "the nominal core is critical, rho ~ 0 (" + round(core.reactivity()) + ")");

        // ---- 2. withdraw rods: power rises on a positive period ----
        core.setRodTarget(0.74);
        run(core, DT, 200);
        System.out.printf("  2 withdraw  : P=%.4f rho=%+.6f%n", core.power(), core.reactivity());
        check(core.power() > 1.0,
            "withdrawing rods raises the power (" + round(core.power()) + ")");

        // ---- 3. reduce power ----
        core.setRodTarget(0.90);
        run(core, DT, 180);
        System.out.printf("  3 reduce    : P=%.4f rho=%+.6f ins=%.3f%n",
            core.power(), core.reactivity(), core.insertion());
        check(core.power() < 0.9, "the power comes down after the rods go in (" + round(core.power()) + ")");
        check(core.power() > 0.01, "the power stays positive, not shut down (" + round(core.power()) + ")");

        // ---- 4. the xenon pit: the poison builds and sags the power ----
        // The Xe-135 is COUPLED (it enters the fuel cell's thermal absorption), so
        // the pit is a real neutron sink, not a display. At a fixed rod position the
        // power sags as the poison builds; the operator must withdraw rods to hold it.
        core.setRodTarget(0.82);
        run(core, DT, 1800);                    // settle to the pre-pit power (~30 min)
        double prePitPower = core.power();
        run(core, DT, 6 * 3600 - 1800);         // let the pit develop
        System.out.printf("  4 xenon pit : Xe=%.3fx  P=%.4f (was %.4f pre-pit)  rho=%+.6f%n",
            core.xenonRatio(), core.power(), prePitPower, core.reactivity());
        check(core.xenonRatio() > 1.05,
            "xenon builds into a pit after the power cut (Xe " + round(core.xenonRatio()) + "x)");
        check(core.power() < prePitPower,
            "the xenon poison sags the power at a fixed rod position ("
                + round(prePitPower) + " -> " + round(core.power()) + ")");

        // ---- 4b. the operator withdraws the rods to the ORM limit to fight the
        // xenon. This is the fatal state: almost all rods out, so the whole core
        // sits on a tiny operating margin (the "ORM of 8"). It is a LEGAL operating
        // configuration - no mode, no flag - and it is where the reactor became
        // dangerous. ----
        core.setRodTarget(0.10);
        run(core, DT, 90);
        System.out.printf("  4b ORM      : P=%.4f rho=%+.6f ins=%.3f%n",
            core.power(), core.reactivity(), core.insertion());
        check(core.insertion() < 0.2,
            "the rods are withdrawn to the top - a low operating margin (ins "
                + round(core.insertion()) + ")");
        check(core.reactivity() > 0,
            "the low-margin core is super-critical (rho " + round(core.reactivity()) + ")");

        // ---- 5. AZ-5: the positive scram, from the LIVE core ----
        // The core is at a legal low-margin operating point. Driving the rods in is
        // what an operator does to shut down. Because the graphite displacer enters
        // the bottom of the channel first, the DERIVED reactivity RISES before it
        // falls - the positive scram that made the power spike at Chernobyl. This is
        // measured from the live core, not read off the fitted rod-worth curve.
        double rhoBefore = core.reactivity();
        double powerBefore = core.power();
        core.setRodTarget(1.0);
        double maxRho = rhoBefore;
        double maxPower = powerBefore;
        int steps = (int) (25 / DT);
        for (int k = 0; k < steps; k++) {
            core.step(DT);
            maxRho = Math.max(maxRho, core.reactivity());
            maxPower = Math.max(maxPower, core.power());
        }
        System.out.printf("  5 AZ-5      : rho %+.6f -> max %+.6f (%+.6f); "
                + "P %.4f -> max %.4f -> %.4f%n",
            rhoBefore, maxRho, maxRho - rhoBefore, powerBefore, maxPower, core.power());
        check(maxRho > rhoBefore,
            "AZ-5 first RAISES reactivity - the positive scram (rho "
                + round(rhoBefore) + " -> " + round(maxRho) + ")");
        check(maxPower > powerBefore,
            "the power rises during the first seconds of the scram ("
                + round(powerBefore) + " -> " + round(maxPower) + ")");
        check(core.power() < maxPower,
            "then the absorber takes over and the power falls ("
                + round(maxPower) + " -> " + round(core.power()) + ")");

        System.out.println();
        System.out.println("AccidentSequence: " + checks + " checks, " + failures + " failures.");
        if (failures > 0) {
            throw new AssertionError(failures + " checks failed");
        }
    }

    private static void run(ReactorModel core, double dt, double seconds) {
        int n = (int) Math.round(seconds / dt);
        for (int k = 0; k < n; k++) {
            core.step(dt);
        }
    }

    private static String round(double v) {
        return String.format("%.5f", v);
    }

    private static void check(boolean cond, String what) {
        checks++;
        if (cond) {
            System.out.println("    [PASS] " + what);
        } else {
            failures++;
            System.out.println("    [FAIL] " + what);
        }
    }
}
