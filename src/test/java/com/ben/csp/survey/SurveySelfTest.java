package com.ben.csp.survey;

import com.ben.csp.prombaza.Area;

/** Standalone self-test for the survey domain. */
public final class SurveySelfTest {
    private static int checks = 0;

    public static void main(String[] args) {
        Area area = Area.of(0, 60, 0, 200, 90, 200);
        Survey s = new Survey("S-1", "geodezist-1", area, "Central Prombaza", 10, 40);

        check(!s.isComplete(), "a new survey is in progress");
        eq(0.0, s.progress(), "no progress at the start");

        s.work(10, 10);
        eq(0.25, s.progress(), "a quarter done after 10 of 40 hours");
        check(!s.isComplete(), "not complete at a quarter");

        s.work(20, 11);
        eq(0.75, s.progress(), "three quarters done");
        check(!s.isComplete(), "still not complete");

        s.work(10, 12);
        check(s.isComplete(), "complete once the hours are done");
        eq(1.0, s.progress(), "progress reaches one");
        eq(12L, s.completedDay(), "completion day recorded");

        // it yields the proposed site
        eq("Central Prombaza", s.proposedName(), "the survey yields the proposed name");
        eq(area, s.area(), "the survey covers the proposed area");

        // guards
        expectThrow(() -> s.work(1, 13), "cannot work a completed survey");
        expectThrow(() -> s.work(-1, 13), "negative hours rejected");
        expectThrow(() -> new Survey("S-2", "g", area, "n", 1, 0),
            "a survey must require some work");

        System.out.println("SurveySelfTest: " + checks + " checks passed.");
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
