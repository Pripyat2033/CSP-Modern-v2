package com.ben.csp.directorate;

import com.ben.csp.prombaza.Area;
import com.ben.csp.enterprise.FacilityType;

/** Standalone self-test for the directorate approval workflow. */
public final class DirectorateSelfTest {
    private static int checks = 0;

    public static void main(String[] args) {
        DirectorateWorkflow wf = new DirectorateWorkflow();
        eq(0, wf.pendingCount(), "in-tray starts empty");

        DirectorateRequest build = new DirectorateRequest("R-1", RequestType.FACILITY_CONSTRUCTION,
            "Raise a concrete batching plant", "zhelezobeton", FacilityType.CONCRETE_PLANT,
            Area.of(100, 60, 100, 120, 75, 120), "dir-zhelezobeton", 40);
        wf.submit(build);
        eq(1, wf.pendingCount(), "one request waiting");
        check(build.isPending(), "request is pending");

        // the director approves it
        wf.approve("R-1", "Ben", 40);
        eq(RequestStatus.APPROVED, build.status(), "approved");
        eq("Ben", build.decidedBy(), "decided by the director");
        eq(0, wf.pendingCount(), "no longer pending");

        // a decision cannot be changed
        expectThrow(() -> wf.deny("R-1", "Ben", 41, "changed my mind"),
            "an already-decided request cannot be re-decided");
        expectThrow(() -> build.approve("Ben", 42), "approving an approved request is refused");

        // a denied request
        DirectorateRequest repair = new DirectorateRequest("R-2", RequestType.FACILITY_REPAIR,
            "Repair the rebar plant roof", "zhelezobeton", FacilityType.REBAR_PLANT,
            null, "dir-zhelezobeton", 41);
        wf.submit(repair);
        wf.deny("R-2", "Ben", 41, "not a priority this quarter");
        eq(RequestStatus.DENIED, repair.status(), "denied");
        eq("not a priority this quarter", repair.decisionReason(), "reason recorded");

        // duplicate id refused; unknown id refused
        expectThrow(() -> wf.submit(new DirectorateRequest("R-1", RequestType.FACILITY_REPAIR,
            "x", null, null, null, "who", 42)), "duplicate request id refused");
        expectThrow(() -> wf.approve("R-99", "Ben", 42), "unknown id refused");

        // the prombaza designation is itself a request the director decides
        DirectorateRequest design = new DirectorateRequest("R-3", RequestType.PROMBAZA_DESIGNATION,
            "Designate the prombaza territory", null, null,
            Area.of(0, 60, 0, 500, 90, 500), "geodezist-1", 42);
        wf.submit(design);
        check(wf.pending().contains(design), "the designation request is in the in-tray");
        wf.approve("R-3", "Ben", 42);
        eq(RequestStatus.APPROVED, design.status(), "designation approved");

        System.out.println("DirectorateSelfTest: " + checks + " checks passed.");
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
