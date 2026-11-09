package com.ben.csp.work;

/** Standalone self-test for the work-measurement domain (no Minecraft, no JUnit). */
public final class WorkSelfTest {

    private static int checks = 0;

    public static void main(String[] args) {
        // norms
        Quantity concrete = Quantity.of(100, WorkUnit.CUBIC_METRE);
        Naryad n = new Naryad("N-001", "Foundations, block A",
            WorkType.CONCRETE_PLACING, concrete, "brigade-1", 1, 10);
        eq(240.0, n.manHoursRequired(), "100 m3 concrete = 240 man-hours");
        eq("100 m³", n.planned().format(), "planned formats with unit symbol");

        // lifecycle
        n.accept();
        eq(Naryad.Status.ACCEPTED, n.status(), "accepted");
        n.recordProgress(Quantity.of(40, WorkUnit.CUBIC_METRE));
        eq(Naryad.Status.IN_PROGRESS, n.status(), "partial -> in progress");
        eq("60 m³", n.remaining().format(), "remaining after 40");
        n.recordProgress(Quantity.of(60, WorkUnit.CUBIC_METRE));
        eq(Naryad.Status.PENDING_INSPECTION, n.status(), "full -> pending inspection");

        // failed inspection sends it back for rework
        n.inspect(false, "honeycombing in lift 3");
        eq(Naryad.Status.REJECTED, n.status(), "failed -> rejected");
        eq("0 m³", n.completed().format(), "rejected resets completed");
        eq(1, n.defectNotes().size(), "one defect note");
        n.reopen();
        eq(Naryad.Status.IN_PROGRESS, n.status(), "reopened -> in progress");

        // redo and pass
        n.recordProgress(Quantity.of(100, WorkUnit.CUBIC_METRE));
        n.inspect(true, null);
        eq(Naryad.Status.COMPLETED, n.status(), "passed -> completed");
        n.close();
        eq(Naryad.Status.CLOSED, n.status(), "closed");

        // overdue
        Naryad late = new Naryad("N-002", "Rebar", WorkType.REBAR_INSTALLATION,
            Quantity.of(5, WorkUnit.TONNE), "brigade-2", 1, 5);
        eq(90.0, late.manHoursRequired(), "5 t rebar = 90 man-hours");
        check(late.isOverdue(6), "overdue after due day");
        check(!late.isOverdue(5), "not overdue on due day");

        // guards
        expectThrow(() -> Quantity.of(-1, WorkUnit.TONNE), "negative quantity rejected");
        expectThrow(() -> Quantity.of(1, WorkUnit.TONNE).plus(Quantity.of(1, WorkUnit.PIECE)),
            "unit mismatch rejected");
        expectThrow(() -> {
            Naryad bad = new Naryad("N-003", "bad", WorkType.REBAR_INSTALLATION,
                Quantity.of(5, WorkUnit.CUBIC_METRE), "b", 1, 2);
        }, "planned unit must match work type unit");
        expectThrow(() -> {
            Naryad notAccepted = new Naryad("N-004", "x", WorkType.EXCAVATION,
                Quantity.of(1, WorkUnit.CUBIC_METRE), "b", 1, 2);
            notAccepted.recordProgress(Quantity.of(1, WorkUnit.CUBIC_METRE));
        }, "cannot record progress before acceptance");

        System.out.println("WorkSelfTest: " + checks + " checks passed.");
    }

    private static void eq(Object expected, Object actual, String what) {
        checks++;
        if (!String.valueOf(expected).equals(String.valueOf(actual))) {
            throw new AssertionError(what + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void check(boolean cond, String what) {
        checks++;
        if (!cond) {
            throw new AssertionError(what);
        }
    }

    private static void expectThrow(Runnable r, String what) {
        checks++;
        try {
            r.run();
        } catch (RuntimeException expected) {
            return;
        }
        throw new AssertionError(what + ": expected an exception, none thrown");
    }
}
