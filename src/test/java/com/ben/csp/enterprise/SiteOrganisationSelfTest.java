package com.ben.csp.enterprise;

import java.util.List;

/** Standalone self-test for the two separate authorities at the site. */
public final class SiteOrganisationSelfTest {
    private static int checks = 0;

    public static void main(String[] args) {
        // the construction line, headed by Kizima, holding the trusts
        ConstructionAdministration construction = new ConstructionAdministration(
            "chpp-construction", "Управление строительства ЧАЭС", HistoricalFigures.KIZIMA);
        List<Enterprise> trusts = EnterpriseRoster.provisional();
        for (Enterprise t : trusts) {
            construction.addTrust(t);
        }

        // the operating line, headed by Bryukhanov
        OperatingEnterprise station = new OperatingEnterprise(
            "chnpp", "Чернобыльская АЭС им. В. И. Ленина", HistoricalFigures.BRYUKHANOV);

        // the two are separate: different ids, different chiefs, different chains
        check(!construction.id().equals(station.id()), "the two lines are different bodies");
        check(!construction.chief().equals(station.director()),
            "the construction chief and the station director are different people");
        eq(HistoricalFigures.KIZIMA, construction.chief(), "Kizima heads the construction line");
        eq(HistoricalFigures.BRYUKHANOV, station.director(), "Bryukhanov heads the station");

        // the trusts sit under the construction administration, not the station
        eq(trusts.size(), construction.trustCount(), "every trust sits under the administration");
        eq(10, construction.trustCount(), "there are ten trusts");

        // the administration is operational only when all its trusts are
        check(!construction.isOperational(), "the administration is not operational yet");
        for (Enterprise t : trusts) {
            for (Facility f : t.facilities()) {
                f.site(Footprint.of(0, 0, 20, 20));
                f.completeConstruction();
                f.assignFacilityDirector("dir-" + f.id());
                f.setStaff(f.requiredStaff());
            }
        }
        check(construction.isOperational(), "the administration is operational once its trusts are");

        System.out.println("SiteOrganisationSelfTest: " + checks + " checks passed.");
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
}
