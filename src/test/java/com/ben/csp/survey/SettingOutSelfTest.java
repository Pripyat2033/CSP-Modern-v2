package com.ben.csp.survey;

import com.ben.csp.enterprise.Footprint;

/** Standalone self-test for the setting-out (разбивка) of a building. */
public final class SettingOutSelfTest {
    private static int checks = 0;

    public static void main(String[] args) {
        Footprint design = Footprint.of(100, 100, 140, 130);
        SettingOut so = new SettingOut("SO-1", "geodezist-1", "facility-hq",
            design, 5, 8);

        check(!so.isComplete(), "a new setting-out is in progress");
        eq(0.0, so.progress(), "no progress at the start");
        eq("facility-hq", so.facilityId(), "it is for one building");

        // it is not sited until the setting-out is done
        expectThrow(so::establishedFootprint, "no footprint before the work is done");

        so.work(4, 5);
        eq(0.5, so.progress(), "half done");
        check(!so.isComplete(), "not complete at half");

        so.work(4, 6);
        check(so.isComplete(), "complete once the hours are done");
        eq(6L, so.completedDay(), "completion day recorded");
        eq(design, so.establishedFootprint(), "it yields the footprint to build at");

        expectThrow(() -> so.work(1, 7), "cannot work a completed setting-out");
        expectThrow(() -> so.work(-1, 7), "negative hours rejected");
        expectThrow(() -> new SettingOut("SO-2", "g", "f", design, 1, 0),
            "a setting-out must require some work");

        System.out.println("SettingOutSelfTest: " + checks + " checks passed.");
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
