package com.ben.csp.survey;

import com.ben.csp.enterprise.Footprint;

/** Standalone self-test for the as-built check (исполнительная съёмка). */
public final class AsBuiltCheckSelfTest {
    private static int checks = 0;

    public static void main(String[] args) {
        Footprint design = Footprint.of(100, 100, 140, 130);

        // built within tolerance: it passes
        Footprint near = Footprint.of(100.2, 99.8, 140.1, 130.3);
        AsBuiltCheck ok = new AsBuiltCheck("AB-1", "g1", "hq", design, near, 0.5, 20, 6);
        check(ok.isWithinTolerance(), "0.3 m is within a 0.5 m tolerance");
        check(Math.abs(ok.deviation() - 0.3) < 1e-9, "deviation is the largest error");
        ok.work(6, 21);
        check(ok.passed(), "it passes once checked");
        eq(21L, ok.completedDay(), "completion day recorded");
        expectThrow(() -> ok.work(1, 22), "cannot check twice");

        // built outside tolerance: it fails
        Footprint off = Footprint.of(101, 100, 141, 130);
        AsBuiltCheck bad = new AsBuiltCheck("AB-2", "g1", "hq", design, off, 0.5, 20, 6);
        check(!bad.isWithinTolerance(), "1 m is outside a 0.5 m tolerance");
        bad.work(3, 21);
        check(!bad.isComplete(), "half the work is not complete");
        bad.work(3, 21);
        check(bad.failed(), "it fails once checked");
        check(bad.isComplete(), "a failed check is still complete");

        // the deviation is symmetric and considers every corner
        Footprint far = Footprint.of(100, 100, 140, 133);
        AsBuiltCheck corner = new AsBuiltCheck("AB-3", "g1", "hq", design, far, 0.5, 20, 2);
        check(Math.abs(corner.deviation() - 3.0) < 1e-9, "a 3 m error on one corner is caught");

        // guards
        expectThrow(() -> new AsBuiltCheck("AB-4", "g", "f", design, near, -1, 1, 2),
            "negative tolerance rejected");
        expectThrow(() -> new AsBuiltCheck("AB-5", "g", "f", design, near, 0.5, 1, 0),
            "a check must require some work");

        System.out.println("AsBuiltCheckSelfTest: " + checks + " checks passed.");
    }

    private static void eq(Object e, Object a, String what) {
        checks++;
        if (!String.valueOf(e).equals(String.valueOf(a))) {
            throw new AssertionError(what + ": expected <" + e + "> but was <" + a + ">");
        }
    }
    private static void check(boolean cond, String what) {
        checks++;
        if (!cond) throw new AssertionError(what);
    }
    private static void expectThrow(Runnable r, String what) {
        checks++;
        try { r.run(); } catch (RuntimeException e) { return; }
        throw new AssertionError(what + ": expected an exception");
    }
}
