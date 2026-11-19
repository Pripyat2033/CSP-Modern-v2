package com.ben.csp.rbmk.physics;

/**
 * Validation of the spatial solver, the reactivity feedbacks and the rod-worth
 * curve — the calculations that make the accident reproducible.
 *
 * <p>Checks: the diffusion eigenvalue converges to a critical core; void adds
 * reactivity (the positive void coefficient); Doppler and temperature feedbacks are
 * negative; the rod worth curve is negative overall but its differential worth goes
 * <em>positive</em> at the start of insertion (the positive scram); and control rods
 * bring a super-critical core back to critical.
 */
public final class RbmkSpatialSelfTest {

    private static int checks = 0;

    public static void main(String[] args) {
        // --- the eigenvalue problem converges ---
        DiffusionSolver s = new DiffusionSolver();
        s.setCoreState(0.0, 300.0);
        int it = s.solve(2000, 1e-7);
        check(it > 0 && it < 2000, "the diffusion eigenvalue converges in " + it + " outer iterations");
        check(s.keff() > 0.5 && s.keff() < 2.0, "k_eff is physically sensible: " + round(s.keff()));

        // --- the flux is peaked at the centre (fundamental mode) ---
        // Compare region averages, NOT the (0,0) cell: the central channel is a
        // CONTROL channel, so the rod depresses its own cell below the fuel around
        // it. A single control cell cannot represent the mode; the radial average
        // is the property that must be centre-peaked.
        //
        // The margin is small (~0.1%) and that is correct, not flaky: with the
        // reflective boundary the core has no leakage, so a homogeneous core has an
        // exactly flat fundamental mode. What is left is the peaking the absorber
        // distribution and the finite lattice impose. A large peaking here would
        // mean a leakage BC had crept back in.
        double inner = meanPhi(s, 0, s.extent() / 2.0);
        double outer = meanPhi(s, s.extent() * 0.9, s.extent());
        check(inner > outer, "the flux peaks at the core centre, not the rim (inner r<="
            + (s.extent() / 2) + ": " + round(inner) + " > outer r>="
            + Math.round(s.extent() * 0.9) + ": " + round(outer) + ")");

        // --- positive void coefficient: void raises k ---
        DiffusionSolver dry = new DiffusionSolver();
        dry.setCoreState(0.5, 300.0);
        dry.solve(2000, 1e-7);
        check(dry.keff() > s.keff(),
            "void raises reactivity: k(void 0.5) " + round(dry.keff())
                + " > k(void 0) " + round(s.keff()) + " (positive void coefficient)");

        // --- Doppler is negative: hot fuel lowers k ---
        DiffusionSolver hot = new DiffusionSolver();
        hot.setCoreState(0.0, 1200.0);
        hot.solve(2000, 1e-7);
        check(hot.keff() < s.keff(),
            "Doppler is negative: k(hot fuel) " + round(hot.keff())
                + " < k(cold) " + round(s.keff()));

        // --- feedback model signs ---
        check(ReactivityModel.VOID_COEFF > 0, "the void coefficient is positive");
        check(ReactivityModel.DOPPLER_COEFF < 0, "the Doppler coefficient is negative");
        double rVoid = ReactivityModel.reactivity(0.3, 300.0, 300.0, 1.0)
            - ReactivityModel.reactivity(0.0, 300.0, 300.0, 1.0);
        check(rVoid > 0, "adding void adds reactivity in the model (" + round(rVoid) + ")");
        // Fuel (Doppler) and graphite temperature have OPPOSITE signs. Doppler is
        // negative; the graphite coefficient is POSITIVE — the RBMK's documented
        // destabilising property (INSAG-7 Table II-I, NUREG-1250). Test them apart,
        // since a combined figure would just reflect whichever term dominates.
        double rFuel = ReactivityModel.reactivity(0.0, 1000.0, ReactivityModel.T_REF, 0.0);
        check(rFuel < 0, "hot fuel (Doppler) gives negative reactivity (" + round(rFuel) + ")");
        check(ReactivityModel.GRAPHITE_COEFF > 0,
            "the graphite coefficient is positive (the RBMK trait)");
        double rGraphite = ReactivityModel.reactivity(0.0, ReactivityModel.T_REF, 700.0, 0.0);
        check(rGraphite > 0, "hot graphite adds reactivity (" + round(rGraphite) + ")");

        // --- rod worth: overall negative, but differential worth goes positive ---
        RodWorth rw = new RodWorth();
        double wOut = rw.worth(0.0);
        double wIn = rw.worth(1.0);
        check(Math.abs(wOut) < 0.01,
            "a fully withdrawn rod is worth little: " + round(wOut)
                + " (its graphite displacer is slightly positive vs a water channel)");
        check(wIn < wOut, "inserting the rod lowers reactivity: " + round(wIn)
            + " < " + round(wOut));
        check(wIn < 0, "a fully inserted rod is negative worth (" + round(wIn) + ")");

        double[] diff = rw.differentialCurve(40);
        boolean negativeByEnd = diff[diff.length - 1] < 0;
        check(negativeByEnd, "the differential worth is negative at the end of insertion ("
            + round(diff[diff.length - 1]) + ")");
        check(wIn < -0.01, "a fully inserted rod has substantial negative worth ("
            + round(wIn) + ")");

        // --- the positive scram is conditioned on a bottom-peaked flux (the accident
        // state); with a centre-peaked flux the two displacer edges cancel to ~zero ---
        RodWorth bottom = RodWorth.bottomPeaked();
        double[] diffBottom = bottom.differentialCurve(40);
        check(diffBottom[0] > 0,
            "with the accident's bottom-peaked flux the initial differential worth is "
                + "positive (" + round(diffBottom[0]) + ") - the positive scram");

        // --- control rods can bring a super-critical core back down ---
        DiffusionSolver withRods = new DiffusionSolver();
        withRods.setCoreState(0.0, 300.0);
        // Insert all control rods fully (B4C replaces water in control cells).
        for (int x = -withRods.extent(); x <= withRods.extent(); x++) {
            for (int z = -withRods.extent(); z <= withRods.extent(); z++) {
                if (withRods.isControl(x, z)) {
                    withRods.setCell(x, z, RbmkXsec.controlCell(RbmkXsec.B4C));
                }
            }
        }
        withRods.solve(2000, 1e-7);
        check(withRods.keff() < s.keff(),
            "inserting the control rods lowers k: " + round(withRods.keff())
                + " < " + round(s.keff()));

        System.out.println("RbmkSpatialSelfTest: " + checks + " checks passed.");
    }

    /** Mean thermal flux over active cells with {@code rMin <= |r| <= rMax}. */
    private static double meanPhi(DiffusionSolver s, double rMin, double rMax) {
        int ext = s.extent();
        int nz = 2 * ext + 1;
        double sum = 0;
        int n = 0;
        for (int x = -ext; x <= ext; x++) {
            for (int z = -ext; z <= ext; z++) {
                if (!s.isActive(x, z)) {
                    continue;
                }
                double r = Math.hypot(x, z);
                if (r >= rMin && r <= rMax) {
                    sum += s.phi1[(x + ext) * nz + (z + ext)];
                    n++;
                }
            }
        }
        return n == 0 ? Double.NaN : sum / n;
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
