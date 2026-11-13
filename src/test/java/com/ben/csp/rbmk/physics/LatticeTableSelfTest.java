package com.ben.csp.rbmk.physics;

/**
 * Self-test for {@link LatticeTable} and the lattice-derived cross sections. It
 * says the core now runs on numbers EARNED from evaluated nuclear data and the
 * real channel geometry, not hand-set fudge factors:
 *
 * <ul>
 *   <li>the derived table is present and loads;</li>
 *   <li>the constants are in physical ranges for an RBMK cell;</li>
 *   <li>the void coefficient is POSITIVE — the trait that defines an RBMK;</li>
 *   <li>{@link RbmkXsec#fuelCell} returns the derived constants, not the
 *       fallback.</li>
 * </ul>
 */
public final class LatticeTableSelfTest {

    private static int checks = 0;

    private static void check(boolean ok, String what) {
        checks++;
        if (!ok) {
            throw new AssertionError("FAILED: " + what);
        }
    }

    public static void main(String[] args) {
        check(LatticeTable.available(), "the lattice table resource loads");
        check(LatticeTable.betaEff() > 0.004 && LatticeTable.betaEff() < 0.009,
            "delayed fraction is physical (" + LatticeTable.betaEff() + ")");

        RbmkXsec water = LatticeTable.cell(0.0);
        RbmkXsec steam = LatticeTable.cell(1.0);
        check(water != null && steam != null, "cell constants at both void ends");
        check(water.d1() > 0.5 && water.d1() < 1.5, "fast diffusion coeff physical (" + water.d1() + ")");
        check(water.d2() > 0.3 && water.d2() < 1.2, "thermal diffusion coeff physical (" + water.d2() + ")");
        check(water.sa2() > 0.001 && water.sa2() < 0.02, "thermal absorption physical (" + water.sa2() + ")");
        check(water.nsf2() > water.nsf1(), "most fission happens on thermal neutrons");
        check(water.ss12() > 0.0, "down-scatter is present (" + water.ss12() + ")");

        // the defining RBMK trait: water -> steam RAISES reactivity
        double kWater = LatticeTable.kInf(0.0);
        double kSteam = LatticeTable.kInf(1.0);
        check(kSteam > kWater,
            "void coefficient is POSITIVE (k_water=" + kWater + " k_steam=" + kSteam + ")");

        // fuelCell must return the derived constants (d1 close to the table)
        RbmkXsec fc = RbmkXsec.fuelCell(0.0, 300.0);
        check(Math.abs(fc.d1() - water.d1()) < 1e-9,
            "fuelCell returns the derived constants, not the fallback");

        // Doppler must be negative (raising fuel temperature lowers reactivity)
        RbmkXsec hot = RbmkXsec.fuelCell(0.0, 1500.0);
        check(hot.sr1() > fc.sr1(), "Doppler adds fast removal when the fuel is hot");

        System.out.println("LatticeTableSelfTest: " + checks + " checks passed.");
    }
}
