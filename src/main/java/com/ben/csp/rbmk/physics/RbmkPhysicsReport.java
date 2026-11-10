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
    }
}
