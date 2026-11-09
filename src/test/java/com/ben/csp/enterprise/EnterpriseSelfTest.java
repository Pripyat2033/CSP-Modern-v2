package com.ben.csp.enterprise;

import java.util.List;

/** Standalone self-test for the enterprise domain. */
public final class EnterpriseSelfTest {
    private static int checks = 0;

    public static void main(String[] args) {
        List<Enterprise> roster = EnterpriseRoster.provisional();
        eq(10, roster.size(), "provisional roster has ten enterprises");
        eq(10L, roster.stream().map(Enterprise::id).distinct().count(), "ids unique");

        Enterprise concrete = roster.stream()
            .filter(e -> e.id().equals("zhelezobeton")).findFirst().orElseThrow();
        eq(4, concrete.requiredFacilities().size(), "concrete trust needs four facilities");
        check(!concrete.isOperational(), "not operational at the start");
        eq(0.0, concrete.readiness(), "readiness starts at zero");

        concrete.markFacilityBuilt(FacilityType.SITE_OFFICE);
        concrete.markFacilityBuilt(FacilityType.STORAGE_YARD);
        eq(0.5, concrete.readiness(), "half the chain is up");
        check(!concrete.isOperational(), "still not operational without plant + rebar");

        concrete.markFacilityBuilt(FacilityType.CONCRETE_PLANT);
        check(concrete.missingFacilities().contains(FacilityType.REBAR_YARD),
            "rebar yard still missing");
        concrete.markFacilityBuilt(FacilityType.REBAR_YARD);
        check(concrete.isOperational(), "operational once the whole chain is up");
        eq(1.0, concrete.readiness(), "readiness reaches one");

        Enterprise transport = roster.stream()
            .filter(e -> e.id().equals("transport")).findFirst().orElseThrow();
        expectThrow(() -> transport.markFacilityBuilt(FacilityType.CONCRETE_PLANT),
            "cannot build an unrequired facility");

        concrete.assignDirector("director-zhelezobeton");
        eq("director-zhelezobeton", concrete.directorId(), "director assigned");
        concrete.setPersonnel(240);
        eq(240, concrete.personnel(), "personnel set");
        expectThrow(() -> concrete.setPersonnel(-1), "negative personnel rejected");

        for (Enterprise e : roster) {
            check(e.requiredFacilities().contains(FacilityType.SITE_OFFICE),
                e.name() + " requires a site office");
        }

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
