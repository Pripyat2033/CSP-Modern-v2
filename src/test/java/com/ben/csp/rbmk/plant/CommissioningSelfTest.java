package com.ben.csp.rbmk.plant;

import java.util.Random;

/**
 * Self-test for commissioning: a built machine is not a working machine. The
 * plant's own arithmetic puts a good machine outside a tight band sometimes, and
 * a defect that входной контроль missed shows up here — or does not, and waits
 * for service.
 */
public final class CommissioningSelfTest {

    private static int checks = 0;

    private static void check(boolean ok, String what) {
        checks++;
        if (!ok) {
            throw new AssertionError(what);
        }
    }

    private static void eq(Object expected, Object actual, String what) {
        checks++;
        if (!expected.equals(actual)) {
            throw new AssertionError(what + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }

    public static void main(String[] args) {
        // --- a clean machine at the tightest method and a wide band passes ---
        var clean = Commissioning.commission(100.0, 0.05, Commissioning.Method.SKALA, 0.0, new Random(1));
        check(clean.passed(), "a clean machine passes a wide band");
        check(!clean.revealedDefect(), "no defect was hiding");

        // --- but even a GOOD machine fails sometimes on the plant's own arithmetic ---
        // Slide rule is coarse (0.3%); a 0.2% band is TIGHTER than its error, so good
        // machines fail. Over many trials, some must fail with no defect present.
        // One Random, drawn sequentially: seeding a fresh Random per iteration with
        // consecutive ints yields near-identical first draws (Java's 48-bit LCG), so
        // every "trial" would be the same draw.
        int goodFailures = 0;
        Random goodRng = new Random(1000);
        for (int i = 0; i < 200; i++) {
            var r = Commissioning.commission(100.0, 0.002, Commissioning.Method.SLIDE_RULE, 0.0,
                goodRng);
            if (!r.passed()) {
                goodFailures++;
            }
        }
        check(goodFailures > 0,
            "a good machine still fails sometimes on a tight band and a slide rule"
                + " (failures=" + goodFailures + ")");

        // --- a defect the input check missed biases the machine and can fail it ---
        int defectFailures = 0;
        Random defectRng = new Random(2000);
        for (int i = 0; i < 200; i++) {
            var r = Commissioning.commission(100.0, 0.02, Commissioning.Method.SKALA, 0.10,
                defectRng);
            if (!r.passed()) {
                defectFailures++;
            }
        }
        check(defectFailures > 0, "a latent defect can fail commissioning");
        var one = Commissioning.commission(100.0, 0.02, Commissioning.Method.SKALA, 0.10, new Random(7));
        check(one.measured() > 100.0, "the defect biases the machine off its drawing value");

        // --- a defect is 'revealed' only when it is what failed the test ---
        var hidden = Commissioning.commission(100.0, 0.05, Commissioning.Method.SKALA, 0.001, new Random(3));
        check(hidden.passed(), "a tiny defect can slip through commissioning too");
        check(!hidden.revealedDefect(), "and is not claimed to be revealed");

        // --- the method ordering is real: Skala is tighter than mental ---
        check(Commissioning.Method.SKALA.error() < Commissioning.Method.SLIDE_RULE.error(),
            "Skala is more precise than a slide rule");
        check(Commissioning.Method.SLIDE_RULE.error() < Commissioning.Method.MENTAL.error(),
            "a slide rule is more precise than mental arithmetic");

        // --- expected value must be positive; tolerance and severity not negative ---
        try {
            Commissioning.commission(0.0, 0.02, Commissioning.Method.SKALA, 0.0, new Random(1));
            check(false, "zero expected must throw");
        } catch (IllegalArgumentException e) {
            check(true, "zero expected throws");
        }
        try {
            Commissioning.commission(100.0, -1.0, Commissioning.Method.SKALA, 0.0, new Random(1));
            check(false, "negative tolerance must throw");
        } catch (IllegalArgumentException e) {
            check(true, "negative tolerance throws");
        }

        System.out.println("CommissioningSelfTest: " + checks + " checks passed.");
    }
}
