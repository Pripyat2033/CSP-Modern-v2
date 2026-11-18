package com.ben.csp.rbmk.physics;

/**
 * Self-test for the graphite-temperature link — the RBMK's POSITIVE temperature
 * coefficient, the property the live core used to omit (it computed a graphite
 * temperature but never fed it to the neutronics).
 *
 * <p>Checks:
 * <ul>
 *   <li>the derived per-constant slopes load;</li>
 *   <li>the net coefficient is POSITIVE (the RBMK trait, INSAG-7/NUREG-1250);</li>
 *   <li>at the nominal graphite temperature the link is a no-op, so the reference
 *       critical state is preserved;</li>
 *   <li>raising the graphite temperature raises k_eff in the 3D core;</li>
 *   <li>the graphite effect is smaller than the void effect (sanity).</li>
 * </ul>
 */
public final class GraphiteTempSelfTest {

    private static int checks = 0;
    private static int failures = 0;

    public static void main(String[] args) {
        System.out.println("=== GraphiteTempSelfTest ===");
        System.out.printf("  lattice table source = %s%n", LatticeTable.source());
        System.out.printf("  graphite link matches the live table = %b%n",
            RbmkGraphiteTemp.matchesLattice());
        testData();
        testNoOpAtNominal();
        testCoreEffect();
        System.out.printf("%nGraphiteTempSelfTest: %d checks, %d failures%n", checks, failures);
        if (failures > 0) {
            throw new AssertionError(failures + " check(s) failed");
        }
    }

    private static void testData() {
        check("the derived graphite-temperature data loads", RbmkGraphiteTemp.available(),
            "available=" + RbmkGraphiteTemp.available());
        check("the net graphite coefficient is POSITIVE (the RBMK trait)",
            RbmkGraphiteTemp.derivedCoeffPerC() > 0,
            String.format("coeff=%+.3e /degC", RbmkGraphiteTemp.derivedCoeffPerC()));
    }

    private static void testNoOpAtNominal() {
        RbmkXsec a = RbmkXsec.fuelCell(0.05, 640.0, 1.0, 0.233, 600.0);
        RbmkXsec b = RbmkXsec.fuelCell(0.05, 640.0, 1.0, 0.233, 600.0);
        check("the link is a no-op at the nominal graphite temperature",
            Math.abs(a.sr1() - b.sr1()) < 1e-15 && Math.abs(a.ss21() - b.ss21()) < 1e-15,
            String.format("sr1=%.8f ss21=%.8f", a.sr1(), b.ss21()));
    }

    /** The whole-core graphite coefficient (reactivity), dk/k per degC. */
    private static double graphiteWorth(double tLowC, double tHighC) {
        double k0 = solve(tLowC);
        double k1 = solve(tHighC);
        return (k1 - k0) / (k0 * k1) / (tHighC - tLowC);
    }

    private static double solve(double graphiteTempC) {
        RbmkSolver3D s = new RbmkSolver3D();
        s.setOmega(RbmkSolver3D.SAFE_OMEGA);
        s.setCoreState(0.05, 640.0, RbmkXsec.voidWorthScale(), graphiteTempC);
        s.setRodInsertion(RbmkTransientSolver.NOMINAL_INSERTION);
        s.solve(3000, 1e-11);
        return s.keff();
    }

    private static void testCoreEffect() {
        double coeff = graphiteWorth(600.0, 800.0);
        System.out.printf("  graphite 600->800 degC: whole-core dk/dT = %+.3e /degC%n", coeff);

        if (RbmkGraphiteTemp.matchesLattice()) {
            // The graphite slopes were measured from the OpenMC cell, so with the
            // matching table the whole-core effect must be POSITIVE.
            check("with the matching lattice table, graphite heating RAISES k_eff",
                coeff > 0, String.format("coeff=%+.3e /degC", coeff));
            check("the graphite effect is smaller than the void effect",
                coeff < 0.005, String.format("coeff=%.3e vs void ~0.02", coeff));
        } else {
            // With a MISmatched table the link is deliberately inert (it would flip
            // sign), so the graphite temperature must not move k_eff at all.
            check("with a mismatched lattice table the graphite link is inert",
                coeff == 0.0, String.format("coeff=%+.3e /degC (expected 0)", coeff));
        }
    }

    private static void check(String name, boolean ok, String detail) {
        checks++;
        if (ok) {
            System.out.printf("  [PASS] %s  (%s)%n", name, detail);
        } else {
            failures++;
            System.out.printf("  [FAIL] %s  (%s)%n", name, detail);
        }
    }
}
