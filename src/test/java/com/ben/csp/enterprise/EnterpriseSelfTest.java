package com.ben.csp.enterprise;

import java.util.List;

/** Standalone self-test for the enterprise / facility domain. */
public final class EnterpriseSelfTest {
    private static int checks = 0;

    public static void main(String[] args) {
        List<Enterprise> roster = EnterpriseRoster.provisional();
        eq(10, roster.size(), "provisional roster has ten enterprises");

        Enterprise concrete = roster.stream()
            .filter(e -> e.id().equals("zhelezobeton")).findFirst().orElseThrow();
        eq(4, concrete.facilities().size(), "concrete trust owns four facilities");

        // the concrete plant: built, but with no facility director yet
        Facility plant = concrete.facility(FacilityType.CONCRETE_PLANT);
        // no building is raised before a geodezist has set it out on the ground
        expectThrow(plant::completeConstruction, "a facility cannot be built before it is set out");
        plant.site(Footprint.of(0, 0, 20, 20));
        plant.completeConstruction();
        plant.setStaff(plant.requiredStaff());
        check(!plant.isOperational(), "a facility with no facility director cannot work");

        plant.assignFacilityDirector("Nikolaev");
        eq("Nikolaev", plant.facilityDirectorId(), "the plant has its own director");
        check(plant.isOperational(), "built, directed and manned plant works");

        // quota + supplies (the facility director's real concerns)
        plant.setQuotaPerDay(100);
        plant.setSupplyPerUnit(1.0);
        plant.deliverSupplies(50);
        plant.work(100);
        eq(50.0, plant.producedToday(), "output limited by supplies on hand");
        check(!plant.isMeetingQuota(), "quota of 100 not met with only 50 produced");
        plant.deliverSupplies(200);
        plant.work(100);
        eq(150.0, plant.producedToday(), "supplies topped up, quota exceeded");
        check(plant.isMeetingQuota(), "quota now met");
        plant.startNewDay();
        eq(0.0, plant.producedToday(), "output resets each day");

        // incidents
        plant.recordIncident("Mixer 2 seized; pour delayed", 12);
        check(plant.hasUnresolvedIncidents(), "the plant has an open incident");
        eq(1, plant.unresolvedIncidents().size(), "one open incident");
        plant.unresolvedIncidents().get(0).resolve();
        check(!plant.hasUnresolvedIncidents(), "incident resolved");

        // build out the whole chain to make the enterprise operational
        for (Facility f : concrete.facilities()) {
            if (!f.hasFootprint()) f.site(Footprint.of(0, 0, 20, 20));
            if (!f.isBuilt()) f.completeConstruction();
            if (f.facilityDirectorId() == null) f.assignFacilityDirector("dir-" + f.id());
            f.setStaff(f.requiredStaff());
        }
        check(concrete.isOperational(), "enterprise operational once every facility works");
        concrete.work(5);
        check(concrete.facility(FacilityType.REBAR_PLANT).produced() > 0, "rebar plant produced");

        // contract + criminal liability (халатность)
        Contract c = new Contract("K-1", "zhelezobeton",
            "Supply 50,000 m3 of concrete by day 300", 300);
        concrete.setContract(c);
        check(!c.isOverdue(299), "not overdue before the due day");
        check(c.isOverdue(301), "overdue after the due day");
        c.recordFailure(CriminalLiability.CRIMINAL_NEGLIGENCE,
            "Wrong concrete mix; foundations condemned");
        concrete.work(1);
        eq(EnterpriseStatus.DELINQUENT, concrete.status(), "failure makes the enterprise delinquent");

        // guards
        expectThrow(() -> new Enterprise("x", "X", Specialty.TRANSPORT, List.of()),
            "an enterprise needs at least one facility");
        expectThrow(() -> plant.setStaff(-1), "negative staff rejected");
        expectThrow(() -> plant.setQuotaPerDay(-1), "negative quota rejected");
        expectThrow(() -> new Contract("k", "e", "d", 1).recordFailure(CriminalLiability.NONE, "x"),
            "a recorded failure must carry liability");

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
