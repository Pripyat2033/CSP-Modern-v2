package com.ben.csp.rbmk.physics;

/**
 * Self-test for the decay-heat model ({@link DecayHeat}). Pins the two facts that
 * matter: (1) at steady state the decay heat is {@link DecayHeat#EQUILIBRIUM} of
 * nominal, so the plant's heat balance is unchanged; (2) after a scram it does NOT
 * go to zero — it follows the Way-Wigner curve {@code 0.065 t^-0.2} (t in seconds).
 * This is the physics that keeps a "shut down" reactor boiling.
 */
public final class DecayHeatSelfTest {

    private static int checks = 0;

    public static void main(String[] args) {
        // --- 1. equilibrium: at full power the decay heat is ~6.5% of nominal ---
        DecayHeat eq = new DecayHeat(1.0);
        check(Math.abs(eq.total() - DecayHeat.EQUILIBRIUM) < 1e-9,
            "at power the decay heat is " + pct(eq.total()) + " of nominal");

        // --- 2. after a scram it decays, never to zero within the run ---
        DecayHeat dh = new DecayHeat(1.0);
        double dt = 0.5;
        double[] marks = {1, 10, 60, 300, 3600, 21600};
        int mi = 0;
        double t = 0;
        System.out.println("  t(s)     decay heat    Way-Wigner");
        while (mi < marks.length) {
            dh.step(dt, 0.0);   // fission stopped
            t += dt;
            if (t >= marks[mi]) {
                double h = dh.total();
                double ww = DecayHeat.EQUILIBRIUM * Math.pow(t, -0.2);
                System.out.printf("  %7.0f   %8.3f%%        %8.3f%%%n", t, h * 100, ww * 100);
                // within a factor of 2 of the Way-Wigner curve (multi-group fit)
                check(h > 0.4 * ww && h < 2.5 * ww,
                    "at t=" + (int) t + "s the decay heat tracks Way-Wigner");
                mi++;
            }
        }

        // --- 3. the total never collapses to zero (the bug Ben saw) ---
        check(dh.total() > 0.005,
            "6 h after shutdown the core is still making heat (" + pct(dh.total()) + ")");

        // --- 4. group shares sum to the equilibrium value ---
        double sum = 0;
        for (double c : DecayHeat.shares()) {
            sum += c;
        }
        check(Math.abs(sum - DecayHeat.EQUILIBRIUM) < 1e-9,
            "the group shares sum to the equilibrium value (" + sum + ")");

        System.out.println("DecayHeatSelfTest: " + checks + " checks passed.");
    }

    private static String pct(double v) {
        return String.format("%.2f%%", v * 100);
    }

    private static void check(boolean cond, String what) {
        checks++;
        if (!cond) {
            throw new AssertionError(what);
        }
    }
}
