package com.ben.csp.planning;

/** Standalone self-test for the planerka meeting model. */
public final class PlanerkaSelfTest {
    private static int checks = 0;

    public static void main(String[] args) {
        // routine planerka: the Director convenes the zone at day 5, 08:00
        Planerka routine = new Planerka("P-1", PlanerkaReason.ROUTINE,
            "director", Authority.DIRECTOR_CHSZO, "zone", 5, 8 * 60);
        eq(Planerka.Status.SCHEDULED, routine.status(), "starts scheduled");

        // cannot open before it is due
        expectThrow(() -> routine.open(5, 7 * 60 + 59), "cannot open before the due moment");
        routine.open(5, 8 * 60);
        eq(Planerka.Status.IN_SESSION, routine.status(), "opens when due");

        routine.addAttendee("sekretar");
        routine.addAttendee("dir-genstroi");
        routine.recordDecision("Brigade 4 to make up 12 m3 of concrete by day 7");
        routine.conclude();
        eq(Planerka.Status.CONCLUDED, routine.status(), "concluded");
        check(routine.minutes().size() >= 3, "minutes recorded");
        check(routine.attendees().contains("sekretar"), "secretary attended");

        // emergency planerka called by an enterprise director for their own unit
        Planerka emergency = new Planerka("P-2", PlanerkaReason.EMERGENCY,
            "dir-zhelezobeton", Authority.ENTERPRISE_DIRECTOR, "zhelezobeton", 5, 14 * 60);
        check(emergency.reason().isUrgent(), "emergency is urgent");
        emergency.open(5, 14 * 60);
        emergency.addAttendee("brigadier-9");
        emergency.summon("prorab-3", "rebar placed off-centre in footing F-12");
        emergency.recordDecision("Footing F-12 to be broken out and recast; prorab to re-inspect");
        emergency.conclude();
        check(emergency.minutes().stream().anyMatch(m -> m.startsWith("Summons")),
            "summons recorded in the minutes");

        // a prorab may NOT convene (they inspect, they do not chair)
        expectThrow(() -> new Planerka("P-3", PlanerkaReason.ROUTINE,
            "prorab-1", Authority.PRORAB, "zone", 5, 8 * 60),
            "a prorab cannot convene a planerka");

        // an enterprise director can convene for their own unit
        Planerka own = new Planerka("P-4", PlanerkaReason.ACCOUNTABILITY,
            "dir-otdelka", Authority.ENTERPRISE_DIRECTOR, "otdelka", 6, 9 * 60);
        own.open(6, 9 * 60);
        own.recordDecision("Finishing behind plan: extra brigade released");
        own.conclude();
        eq(Planerka.Status.CONCLUDED, own.status(), "unit planerka concluded");

        // guards
        expectThrow(() -> routine.recordDecision("too late"), "cannot decide after conclusion");
        Planerka cancelled = new Planerka("P-5", PlanerkaReason.ROUTINE,
            "director", Authority.DIRECTOR_CHSZO, "zone", 7, 8 * 60);
        cancelled.cancel();
        eq(Planerka.Status.CANCELLED, cancelled.status(), "cancelled");
        expectThrow(() -> cancelled.addAttendee("x"), "cannot attend a cancelled planerka");

        System.out.println("PlanerkaSelfTest: " + checks + " checks passed.");
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
