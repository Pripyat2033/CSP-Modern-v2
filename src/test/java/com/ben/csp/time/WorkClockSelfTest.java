package com.ben.csp.time;

/** Standalone self-test for {@link WorkClock}. */
public final class WorkClockSelfTest {
    private static int checks = 0;

    public static void main(String[] args) {
        WorkClock c = WorkClock.fresh(0L);
        eq(1L, c.workDay(), "starts on day 1");
        eq("00:00", c.hhmm(), "starts at midnight");

        c.tick(30L * 60 * 1000);
        eq("12:00", c.hhmm(), "half an hour of real time = 12:00 work time");
        eq(1L, c.workDay(), "still day 1");

        long crossed = c.tick(60L * 60 * 1000);
        eq(1L, crossed, "crossed one day boundary");
        eq(2L, c.workDay(), "now day 2");
        eq("00:00", c.hhmm(), "back to midnight");

        WorkClock s = WorkClock.fresh(0L);
        s.addWorkMinutes(7 * 60 + 59);
        check(!s.isShift(), "07:59 off shift");
        s.addWorkMinutes(1);
        check(s.isShift(), "08:00 on shift");
        s.addWorkMinutes(12 * 60);
        check(!s.isShift(), "20:00 off shift");

        WorkClock t = WorkClock.fresh(1000L);
        eq(0L, t.tick(500L), "clock does not go backwards");

        WorkClock offline = WorkClock.fresh(0L);
        long days = offline.addWorkMinutes(WorkClock.MINUTES_PER_DAY * 3.5);
        eq(3L, days, "3.5 days crosses three boundaries");
        eq(4L, offline.workDay(), "landed on day 4");
        eq("12:00", offline.hhmm(), "at 12:00");

        WorkClock r = WorkClock.fresh(0L);
        r.setRate(WorkClock.MINUTES_PER_DAY / 60.0);
        r.tick(60L * 1000);
        eq("00:00", r.hhmm(), "one real minute = one full day");
        eq(2L, r.workDay(), "advanced a day");

        expectThrow(() -> WorkClock.fresh(0L).setRate(0), "rate must be positive");
        expectThrow(() -> new WorkClock(0L, 1.0, 0), "day is 1-based");

        System.out.println("WorkClockSelfTest: " + checks + " checks passed.");
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
