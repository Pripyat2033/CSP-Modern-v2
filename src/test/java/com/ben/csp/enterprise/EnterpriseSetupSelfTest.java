package com.ben.csp.enterprise;

/** Standalone self-test for the enterprise HQ dependency. */
public final class EnterpriseSetupSelfTest {
    private static int checks = 0;

    public static void main(String[] args) {
        EnterpriseSetup s = new EnterpriseSetup("genstroi");
        check(!s.hasHq(), "no HQ at the start");
        eq(0, s.hqProgress(), "no construction progress yet");

        // nothing works without an HQ
        expectThrow(() -> s.hire(5), "cannot hire without an HQ");
        expectThrow(() -> s.dismiss(1), "cannot dismiss without an HQ");
        expectThrow(() -> s.completeDesignContract(), "cannot complete design contracts without an HQ");
        expectThrow(() -> s.upgrade(), "cannot upgrade without an HQ");

        // the enterprise builds its own HQ
        s.beginHqConstruction();
        s.advanceHqConstruction(60);
        eq(60, s.hqProgress(), "HQ half-built");
        check(!s.hasHq(), "still no HQ at 60%");
        s.advanceHqConstruction(50);   // clamps to 100
        eq(100, s.hqProgress(), "progress clamps at 100");
        check(s.hasHq(), "HQ complete");

        // now the office exists and work can begin
        s.hire(12);
        eq(12, s.staffHired(), "staff hired");
        s.dismiss(2);
        eq(10, s.staffHired(), "staff dismissed");

        // upgrading needs staff and design work
        expectThrow(() -> s.upgrade(), "cannot upgrade with too few design contracts");
        s.completeDesignContract();
        s.upgrade();
        eq(2, s.level(), "upgraded to level 2");
        expectThrow(() -> s.upgrade(), "level 2 needs more staff to go further");

        // guards
        expectThrow(() -> s.advanceHqConstruction(10), "cannot advance a finished HQ");
        expectThrow(() -> s.dismiss(999), "cannot dismiss more than you have");
        expectThrow(() -> s.hire(-1), "cannot hire a negative number");

        System.out.println("EnterpriseSetupSelfTest: " + checks + " checks passed.");
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
