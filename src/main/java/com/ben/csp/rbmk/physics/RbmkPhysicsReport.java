package com.ben.csp.rbmk.physics;

/**
 * Prints the classic reactor calculations the way a station would — the sort of
 * printout an operator or the SKALA computer produced. Run it to see the same
 * numbers a real RBMK engineer worked with.
 */
public final class RbmkPhysicsReport {

    public static void main(String[] args) {
        System.out.println("=== RBMK-1000 physics: classic calculations ===");
        System.out.printf("beta_eff = %.4f   Lambda = %.1f us%n",
            DelayedNeutrons.BETA_EFFECTIVE, DelayedNeutrons.GENERATION_TIME * 1e6);
        System.out.println("delayed groups (lambda_i, beta_i):");
        for (int i = 0; i < DelayedNeutrons.GROUPS; i++) {
            System.out.printf("  %d: lambda=%.4f 1/s   beta_i=%.5f%n",
                i + 1, DelayedNeutrons.LAMBDA[i], DelayedNeutrons.beta(i));
        }

        System.out.println();
        System.out.println("Startup chart (inhour): reactivity -> stable period");
        System.out.println("  rho (dk/k)   rho (dollars)     period T (s)");
        double[] rhos = {0.0001, 0.0002, 0.0005, 0.001, 0.002, 0.003, 0.005, 0.0065};
        for (double r : rhos) {
            double dollars = r / DelayedNeutrons.BETA_EFFECTIVE;
            double T = PointKinetics.period(r);
            System.out.printf("  %.5f        %6.2f         %8.2f%n", r, dollars, T);
        }
        System.out.printf("  rho = beta (1 dollar): the period is prompt-dominated, T ~ %.1f s%n",
            PointKinetics.asymptoticPeriod(DelayedNeutrons.BETA_EFFECTIVE));

        System.out.println();
        System.out.println("Power curve for a step insertion of +0.0005 dk/k:");
        PointKinetics pk = new PointKinetics();
        pk.setReactivity(0.0005);
        double dt = 0.05;
        double t = 0;
        System.out.println("   t (s)      P/P0");
        for (int k = 0; k < 40; k++) {
            pk.step(dt);
            t += dt;
            if (k % 4 == 0) {
                System.out.printf("  %6.1f    %8.3f%n", t, pk.power());
            }
        }

        System.out.println();
        System.out.println("Xenon pit: power cut to 10%, xenon vs full-power equilibrium:");
        Xenon xe = new Xenon(1.0);
        double eq = xe.xenon();
        xe.setFlux(0.1);
        System.out.println("   t (h)     Xe/Xe_eq");
        for (int h = 1; h <= 24; h++) {
            for (int m = 0; m < 60; m++) {
                xe.step(60.0);
            }
            if (h % 2 == 0) {
                System.out.printf("  %5d    %8.3f%n", h, xe.xenon() / eq);
            }
        }

        spatial();
    }

    private static void spatial() {
        System.out.println();
        System.out.println("Spatial two-group diffusion (k_eff of the lattice):");
        DiffusionSolver fresh = new DiffusionSolver();
        fresh.setCoreState(0.0, 300.0);
        fresh.solve(2000, 1e-7);
        System.out.printf("  fresh core, no void          : k_eff = %.5f%n", fresh.keff());
        DiffusionSolver voided = new DiffusionSolver();
        voided.setCoreState(0.5, 300.0);
        voided.solve(2000, 1e-7);
        System.out.printf("  with 50%% void (positive coeff): k_eff = %.5f  (%+.5f)%n",
            voided.keff(), voided.keff() - fresh.keff());
        DiffusionSolver rods = new DiffusionSolver();
        rods.setCoreState(0.0, 300.0);
        for (int x = -rods.extent(); x <= rods.extent(); x++) {
            for (int z = -rods.extent(); z <= rods.extent(); z++) {
                if (rods.isControl(x, z)) {
                    rods.setCell(x, z, RbmkXsec.controlCell(RbmkXsec.B4C));
                }
            }
        }
        rods.solve(2000, 1e-7);
        System.out.printf("  all control rods inserted    : k_eff = %.5f  (%+.5f)%n",
            rods.keff(), rods.keff() - fresh.keff());

        System.out.println();
        System.out.println("Rod worth curve (axial flux shape, integral and differential):");
        RodWorth centre = new RodWorth();
        RodWorth bottom = RodWorth.bottomPeaked();
        double[] ic = centre.integralCurve(10);
        double[] id = bottom.integralCurve(10);
        double[] dc = centre.differentialCurve(10);
        double[] dd = bottom.differentialCurve(10);
        System.out.println("  ins   W_centre   dW_centre    W_bottom   dW_bottom");
        for (int i = 0; i <= 10; i++) {
            double dC = i < 10 ? dc[i] : 0;
            double dB = i < 10 ? dd[i] : 0;
            System.out.printf("  %.1f   %+.5f   %+.5f     %+.5f   %+.5f%n",
                i / 10.0, ic[i], dC, id[i], dB);
        }
        System.out.println("  (bottom-peaked = accident state: dW starts POSITIVE -> positive scram)");
    }
}
