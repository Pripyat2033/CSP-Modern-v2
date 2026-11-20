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

        // ---- 4. the xenon pit ----
        // The Xe-135 is COUPLED (it enters the fuel cell's thermal absorption), so
        // the pit is a real neutron sink, not a display.
        //
        // The pit only forms at LOW flux and it takes HOURS. Xe-135 is produced from
        // I-135 (T1/2 6.57 h) and burned off by the flux itself, so while the power
        // is high the burn-off hides the production; hold the reactor down and the
        // iodine decays into xenon that nothing is destroying. The operators held
        // ~200 MWt for hours between 00:28 and 01:23, so that is what is modelled:
        // the regulator holds 200 MWt (6.25 % of nominal) while the poison builds.
        //
        // ORDER MATTERS: setAutoRegulator latches the setpoint to the CURRENT power,
        // so the setpoint must be set AFTER the regulator is engaged. Called the
        // other way round the regulator holds whatever the power was - which after
        // the pit is zero, and "holding zero" parks the core dead.
        core.setAutoRegulator(true);
        core.setPowerSetpoint(200.0 / 3200.0);
        double insAtPitStart = core.insertion();
        run(core, DT, 6 * 3600);
        double pitPower = core.power();
        System.out.printf("  4 xenon pit : Xe=%.3fx  P=%.4f  ins %.3f -> %.3f  rho=%+.6f%n",
            core.xenonRatio(), pitPower, insAtPitStart, core.insertion(), core.reactivity());
        check(core.xenonRatio() > 1.05,
            "xenon builds into a pit after the power cut (Xe " + round(core.xenonRatio()) + "x)");
        check(core.insertion() < insAtPitStart,
            "the regulator withdraws rods to fight the poison (ins "
                + round(insAtPitStart) + " -> " + round(core.insertion()) + ")");
        check(pitPower > 0.01,
            "the core is still alive at ~200 MWt, not shut down (" + round(pitPower) + ")");

        // ---- 4a. the poison's pull, measured with the regulator off ----
        // Hold the rods still for ten minutes and watch the power sag: that is the
        // poison acting on the neutron balance with nothing fighting back. Kept short
        // on purpose - a core left at fixed rods through the whole pit simply dies,
        // and a dead core cannot be scrammed, so there would be nothing left to test.
        core.setAutoRegulator(false);
        double sagFrom = core.fissionPower();
        run(core, DT, 600);
        double sagTo = core.fissionPower();
        System.out.printf("  4a poison   : P_fis %.5f -> %.5f in 10 min at fixed rods%n",
            sagFrom, sagTo);
        check(sagTo < sagFrom,
            "the xenon poison sags the power at a fixed rod position ("
                + round(sagFrom) + " -> " + round(sagTo) + ")");

        // ---- 4b. the operator withdraws the rods to the ORM limit to fight the
        // xenon. This is the fatal state: almost all rods out, so the whole core
        // sits on a tiny operating margin (the "ORM of 8"). It is a LEGAL operating
        // configuration - no mode, no flag - and it is where the reactor became
        // dangerous. ----
        // The regulator comes OFF here: the operator is taking manual control to make
        // the test, which is what happened. Left on, АР would drive the rods back in
        // against every withdrawal and against the scram itself, and the sequence
        // would be measuring the regulator, not the reactor.
        core.setAutoRegulator(false);
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
        //
        // The power watched here is FISSION power, not power(). At ~6 % the total is
        // dominated by decay heat (DecayHeat.EQUILIBRIUM = 6.5 % of nominal, which
        // does not care about the flux at all), so a neutron excursion of tens of
        // percent moves power() by almost nothing - the first version of this check
        // compared 0.00768 with 0.00768 and failed for that reason alone. The
        // excursion is a neutron effect and must be measured on the neutron term.
        double rhoBefore = core.reactivity();
        double powerBefore = core.fissionPower();
        core.setRodTarget(1.0);
        double maxRho = rhoBefore;
        double maxPower = powerBefore;
        int steps = (int) (25 / DT);
        for (int k = 0; k < steps; k++) {
            core.step(DT);
            maxRho = Math.max(maxRho, core.reactivity());
            maxPower = Math.max(maxPower, core.fissionPower());
        }
        System.out.printf("  5 AZ-5      : rho %+.6f -> max %+.6f (%+.6f); "
                + "P_fis %.4f -> max %.4f -> %.4f%n",
            rhoBefore, maxRho, maxRho - rhoBefore, powerBefore, maxPower, core.fissionPower());
        check(maxRho > rhoBefore,
            "AZ-5 first RAISES reactivity - the positive scram (rho "
                + round(rhoBefore) + " -> " + round(maxRho) + ")");
        check(maxPower > powerBefore,
            "the fission power rises during the first seconds of the scram ("
                + round(powerBefore) + " -> " + round(maxPower) + ")");
        check(core.fissionPower() < maxPower,
            "then the absorber takes over and the fission power falls ("
                + round(maxPower) + " -> " + round(core.fissionPower()) + ")");

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
