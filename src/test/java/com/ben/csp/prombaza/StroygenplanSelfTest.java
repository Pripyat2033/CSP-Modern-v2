package com.ben.csp.prombaza;

import com.ben.csp.enterprise.Provenance;

/** Standalone self-test for the construction site plan (стройгенплан). */
public final class StroygenplanSelfTest {
    private static int checks = 0;

    public static void main(String[] args) {
        Stroygenplan plan = PrombazaFacts.siteplan("pb1");

        // the position is the known fact
        eq("pb1", plan.prombazaId(), "the plan belongs to a prombaza");
        eq(Provenance.VERIFIED, plan.positionProvenance(), "the position is a cited fact");
        check(plan.positionNote().contains("west of Unit 4"), "the position is west of Unit 4");
        check(plan.positionSource().length() > 0, "the position cites its source");

        // the layout is a reconstruction: nothing on it is claimed as history
        check(plan.size() >= 8, "the site plan lists the site's facilities");
        check(plan.isAllReconstruction(), "the whole layout is marked reconstruction");
        eq(0L, plan.verifiedCount(), "no facility on it is claimed as verified");
        for (SiteFacility f : plan.facilities()) {
            eq(Provenance.RECONSTRUCTION, f.provenance(), f.name() + " is a reconstruction");
        }

        // it carries the kinds a real site needs
        check(plan.facilities().stream().anyMatch(f -> f.kind() == SiteFacilityKind.RAILHEAD),
            "a railhead is on the plan");
        check(plan.facilities().stream().anyMatch(f -> f.kind() == SiteFacilityKind.CANTEEN),
            "a canteen is on the plan");

        // a facility may be promoted to verified when a source appears
        Stroygenplan p2 = new Stroygenplan("pb2", "somewhere", Provenance.RECONSTRUCTION, "none");
        p2.add("Office", SiteFacilityKind.OFFICE);
        check(p2.isAllReconstruction(), "a fresh plan is all reconstruction");
        p2.addVerified("Known store", SiteFacilityKind.WAREHOUSE);
        eq(1L, p2.verifiedCount(), "a sourced facility is counted as verified");
        check(!p2.isAllReconstruction(), "the plan is no longer all reconstruction");

        System.out.println("StroygenplanSelfTest: " + checks + " checks passed.");
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
