package com.ben.csp.prombaza;

import com.ben.csp.enterprise.Provenance;

/**
 * What is actually known about the ChNPP prombaza, kept apart from what is
 * reconstructed. The only solid fact is its position.
 */
public final class PrombazaFacts {

    private PrombazaFacts() {}

    /** Source for the position: the developer's own knowledge of the site. */
    public static final String POSITION_SOURCE =
        "Ben (Pripyat2033), personal knowledge of the site";

    /** The prombaza lay west of Unit 4, behind the plant perimeter fence. */
    public static final String POSITION_NOTE =
        "west of Unit 4's western wall, behind the plant perimeter fence, "
        + "visible from Unit 4's upper corridors";

    /** A site plan for the prombaza: position known, layout reconstructed. */
    public static Stroygenplan siteplan(String prombazaId) {
        Stroygenplan plan = new Stroygenplan(prombazaId, POSITION_NOTE,
            Provenance.VERIFIED, POSITION_SOURCE);
        plan.add("Site administration office", SiteFacilityKind.OFFICE);
        plan.add("Records archive", SiteFacilityKind.ARCHIVE);
        plan.add("Materials warehouse", SiteFacilityKind.WAREHOUSE);
        plan.add("Site canteen", SiteFacilityKind.CANTEEN);
        plan.add("Repair workshop", SiteFacilityKind.WORKSHOP);
        plan.add("Railhead", SiteFacilityKind.RAILHEAD);
        plan.add("Truck park", SiteFacilityKind.TRUCK_PARK);
        plan.add("Crane tracks", SiteFacilityKind.CRANE_TRACK);
        plan.add("Site roads", SiteFacilityKind.ROAD);
        plan.add("Main gate", SiteFacilityKind.GATE);
        return plan;
    }
}
