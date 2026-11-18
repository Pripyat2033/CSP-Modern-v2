package com.ben.csp.rbmk.physics;

/**
 * The paranoia test: does the model DISCRIMINATE between the experiment variants,
 * or does it just "run and hold" whatever it is given?
 *
 * <p>The historical objection is the whole point of the project: the turbine
 * rundown (run-down) test had been performed at Chernobyl in every unit, in prior
 * years, and it did not blow up. So if a model cannot tell the April-26 run apart
 * from those earlier runs, the model is worthless — it has "proved" nothing about
 * what was different that night.
 *
 * <p>This test runs the SAME AZ-5 scram, through the SAME code path, from the two
 * starting states that actually differ between those runs, and shows the outcomes
 * are opposite:
 *
 * <ul>
 *   <li><b>Variant A — a normal run.</b> Rods partly in (a healthy ORM), the core
 *       critical at power. AZ-5 drives the rods in and the power falls: the reactor
 *       shuts down. This is the earlier tests.</li>
 *   <li><b>Variant B — the night of 26 April.</b> Rods nearly all withdrawn (ORM
 *       violated), the core at low power in a xenon-poisoned state. AZ-5 drives the
 *       rods in and the power first RISES — the positive scram, from the graphite
 *       displacers entering a bottom-peaked flux — and the core does not shut
 *       down.</li>
 * </ul>
 *
 * <p>The difference is NOT a flag in the test. It is the rod insertion fed to the
 * same solver, and the effect is the physics: the rod's axial geometry
 * (B4C / water telescope / graphite displacer, from
 * {@link RbmkGeometry#rodMaterials}) meeting a bottom-peaked axial flux. If the
 * model were fitted to "the answer", it could not produce two different answers
 * from the same code and the same command.
 */
public final class ExperimentVariantsSelfTest {

    private static int checks = 0;
    private static int failures = 0;

    public static void main(String[] args) {
        System.out.println("ExperimentVariantsSelfTest: does the model tell the runs apart?");

        // The two runs differ ONLY in where the rods start and how the core is
        // poisoned. Everything else — the AZ-5 command, the rod speed, the solver —
        // is identical.
        Result normal = az5("A  normal run        (rods IN,  healthy ORM)",
            0.75, 0.05, 1.00);
        Result accident = az5("B  26 April 1986     (rods OUT, ORM violated)",
            0.08, 0.10, 0.06);

        // ---- the discrimination itself ----
        check(normal.peak <= normal.p0 * 1.02,
            "normal run: AZ-5 does NOT raise the power (" + pctS(normal) + "%)");
        check(accident.peak > accident.p0 * 1.10,
            "26 April: AZ-5 RAISES the power (" + pctS(accident) + "%)");

        check(normal.end < normal.p0 * 0.60,
            "normal run: AZ-5 shuts the core down (" + round(normal.end) + ")");
        // "Does not shut down" means the power stays at many times nominal, NOT
        // that it is still rising. With the derived control-rod materials a full
        // scram does drive k_eff subcritical (the rods work), but it does so only
        // after a large positive-scram excursion, so 12 s in the power is still
        // several times nominal — the core never returned to a safe low power.
        check(accident.end > 3.0,
            "26 April: AZ-5 does NOT shut the core down (" + round(accident.end)
                + "x nominal)");
        check(accident.end > normal.end * 5.0,
            "26 April ends far above the normal run ("
                + round(accident.end) + " vs " + round(normal.end) + ")");

        // The one number that separates the two nights:
        double gap = pct(accident) - pct(normal);
        check(gap > 20,
            "the two runs are separated by the scram response, not by a flag "
            + "(" + pctS(normal) + " % vs " + pctS(accident) + " %)");

        // ---- and the mechanism, not just the outcome ----
        // The positive scram is a rod-position effect: it must be visible as the
        // k_eff response to moving the rods IN at low insertion.
        double kOut = keffAt(0.00, 0.10);
        double kIn = keffAt(0.20, 0.10);
        check(kIn > kOut,
            "at low insertion, driving the rods IN raises k_eff: the graphite "
            + "displacer (" + round(kOut) + " -> " + round(kIn) + ")");

        System.out.printf("%nExperimentVariantsSelfTest: %d checks, %d failures%n",
            checks, failures);
        if (failures > 0) {
            System.exit(1);
        }
    }

    // ---------------------------------------------------------------- helpers

    /** A run: seed the core at a state, apply AZ-5, watch the first 12 s. */
    private static Result az5(String name, double ins, double voidFrac, double p) {
        RbmkTransientSolver s = new RbmkTransientSolver();
        // Settle to the variant's operating point (rods, void, fuel temperature).
        double fuel = 640.0 + 600.0 * (p - 1.0);
        for (int k = 0; k < 400; k++) {
            s.setCoreState(voidFrac, fuel);
            s.setRodInsertion(ins);
            s.step(0.05, 2);
        }
        double p0 = s.power();
        // AZ-5: the rods drive in at the real emergency speed (~0.2 insertion/s).
        double peak = p0;
        for (int k = 0; k < 240; k++) {
            double target = Math.min(1.0, ins + 0.2 * (k * 0.05));
            s.setCoreState(voidFrac, fuel);
            s.setRodInsertion(target);
            s.step(0.05, 2);
            peak = Math.max(peak, s.power());
        }
        double end = s.power();
        System.out.printf("  %-42s P0=%6.3f  peak=%6.3f (%+6.1f%%)  end=%6.3f%n",
            name, p0, peak, (peak - p0) / p0 * 100.0, end);
        return new Result(p0, peak, end);
    }

    /** k_eff with the rods at a given insertion, at the accident void. */
    private static double keffAt(double ins, double voidFrac) {
        RbmkTransientSolver s = new RbmkTransientSolver();
        for (int k = 0; k < 400; k++) {
            s.setCoreState(voidFrac, 640.0);
            s.setRodInsertion(ins);
            s.step(0.05, 2);
        }
        return s.balanceKeff();
    }

    private static double pct(Result r) {
        return (r.peak - r.p0) / r.p0 * 100.0;
    }

    private static String pctS(Result r) {
        return String.format("%.3f", pct(r));
    }

    private static String round(double v) {
        return String.format("%.3f", v);
    }

    private static void check(boolean ok, String what) {
        checks++;
        if (ok) {
            System.out.println("  [PASS] " + what);
        } else {
            failures++;
            System.out.println("  [FAIL] " + what);
        }
    }

    private record Result(double p0, double peak, double end) {}
}
