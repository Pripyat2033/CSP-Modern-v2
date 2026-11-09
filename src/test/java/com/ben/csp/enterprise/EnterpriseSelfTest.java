package com.ben.csp.enterprise;

import java.util.List;

/** Standalone self-test for the enterprise / facility domain. */
public final class EnterpriseSelfTest {
    private static int checks = 0;

    public static void main(String[] args) {
        List<Enterprise> roster = EnterpriseRoster.provisional();
        eq(10, roster.size(), "provisional roster has ten enterprises");
        eq(10L, roster.stream().map(Enterprise::id).distinct().count(), "ids unique");

        Enterprise concrete = roster.stream()
            .filter(e -> e.id().equals("zhelezobeton")).findFirst().orElseThrow();
        eq(4, concrete.facilities().size(), "concrete trust owns four facilities");
        check(!concrete.isOperational(), "not operational at the start");
        eq(0.0, concrete.readiness(), "readiness starts at zero");

        // a facility is inert until built, directed and manned
        Facility plant = concrete.facility(FacilityType.CONCRETE_PLANT);
        check(plant != null, "concrete trust has a batching plant");
        check(!plant.isOperational(), "unbuilt plant does not work");
        plant.work(10);
        eq(0.0, plant.produced(), "unbuilt plant produces nothing");

        plant.completeConstruction();
        plant.assignDirector("director-plant");
        plant.setStaff(30);
        check(!plant.isOperational(), "under-manned plant still idle");
        plant.setStaff(60);
        check(plant.isOperational(), "built, directed and manned plant works");
        plant.work(10);
        eq(10.0, plant.produced(), "operational plant produces with the clock");

        // build out the whole chain
        for (Facility f : concrete.facilities()) {
            if (!f.isBuilt()) f.completeConstruction();
            f.assignDirector("dir-" + f.id());
            f.setStaff(f.requiredStaff());
        }
        check(concrete.isOperational(), "enterprise operational once every facility works");
        eq(1.0, concrete.readiness(), "readiness reaches one");
        concrete.work(5);
        check(concrete.facility(FacilityType.REBAR_PLANT).produced() > 0, "rebar plant produced");

        // contract + criminal liability (халатность)
        Contract c = new Contract("K-1", "zhelezobeton",
            "Supply 50,000 m3 of concrete by day 300", 300);
        concrete.setContract(c);
        check(!c.isOverdue(299), "not overdue before the due day");
        check(c.isOverdue(301), "overdue after the due day");
        c.recordFailure(CriminalLiability.CRIMINAL_NEGLIGENCE,
            "Concrete supplied to a wrong mix; foundations condemned");
        concrete.work(1); // refresh status
        eq(EnterpriseStatus.DELINQUENT, concrete.status(), "failure makes the enterprise delinquent");
        eq(CriminalLiability.CRIMINAL_NEGLIGENCE, c.liability(), "criminal liability recorded");

        // an enterprise that fulfils is not delinquent
        Contract ok = new Contract("K-2", "transport", "Haul spoil by day 100", 100);
        ok.fulfil();
        check(!ok.isOverdue(500), "fulfilled contract never overdue");
        expectThrow(() -> ok.recordFailure(CriminalLiability.NONE, "x"),
            "a recorded failure must carry liability");

        // guards
        expectThrow(() -> new Enterprise("x", "X", Specialty.TRANSPORT, List.of()),
            "an enterprise needs at least one facility");
        expectThrow(() -> plant.setStaff(-1), "negative staff rejected");
        expectThrow(() -> new Facility("f", FacilityType.WORKSHOP, "w", -1),
            "negative required staff rejected");

        System.out.println("EnterpriseSelfTest: " + checks + " checks passed.");
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
