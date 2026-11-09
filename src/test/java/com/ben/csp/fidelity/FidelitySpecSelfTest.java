package com.ben.csp.fidelity;

import com.ben.csp.enterprise.Footprint;

/** Standalone self-test for the fidelity spec (exact-scale design dimensions). */
public final class FidelitySpecSelfTest {
    private static int checks = 0;

    public static void main(String[] args) {
        FidelitySpec spec = new FidelitySpec("RB-A", "Reactor building block A",
            66.0, 132.0, 0.05, "drawing RB-A-01");

        // built to the design
        Footprint exact = Footprint.of(0, 0, 66.0, 132.0);
        eq(0.0, spec.widthError(exact), "no width error when built to size");
        eq(0.0, spec.lengthError(exact), "no length error when built to size");
        check(spec.isWithinTolerance(exact), "a structure built to the design is within tolerance");

        // built 4 cm out: still inside a 5 cm tolerance
        Footprint close = Footprint.of(0, 0, 66.04, 132.0);
        check(spec.isWithinTolerance(close), "4 cm is inside a 5 cm tolerance");

        // built 10 cm out: refused
        Footprint off = Footprint.of(0, 0, 66.10, 132.0);
        check(!spec.isWithinTolerance(off), "10 cm is outside a 5 cm tolerance");
        check(Math.abs(spec.worstError(off) - 0.10) < 1e-9, "the worst error is reported");

        // a length error alone is caught
        Footprint shortBy = Footprint.of(0, 0, 66.0, 131.5);
        check(!spec.isWithinTolerance(shortBy), "a half-metre shortfall is caught");

        // guards: a spec needs real dimensions and a source
        expectThrow(() -> new FidelitySpec("x", "X", 0, 10, 0.1, "d"), "zero width rejected");
        expectThrow(() -> new FidelitySpec("x", "X", 10, 10, -1, "d"), "negative tolerance rejected");
        expectThrow(() -> new FidelitySpec("x", "X", 10, 10, 0.1, null), "a source is required");

        System.out.println("FidelitySpecSelfTest: " + checks + " checks passed.");
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
