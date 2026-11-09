package com.ben.csp.enterprise;

import java.util.List;

/**
 * The enterprises that build the zone, each with the real facilities it must
 * construct, staff and run.
 *
 * <p>PROVENANCE. Only the general contractor is verified: the station was built
 * by the trust Кременчуггэсстрой (ru.wikipedia, Чернобыльская АЭС). That one
 * enterprise is marked VERIFIED and carries the real name. The specialist trusts
 * below are RECONSTRUCTIONS: historically plausible for a large Soviet site, but
 * the record does not name the actual ChNPP subcontractors, so they are marked as
 * provisional and may be renamed or replaced as data appears. Keep ids.
 */
public final class EnterpriseRoster {

    private EnterpriseRoster() {}

    private static Facility fac(String entId, FacilityType type, String label, int staff) {
        return new Facility(entId + "-" + type.name().toLowerCase(), type, label, staff);
    }

    private static Enterprise of(String id, String name, Specialty specialty,
                                 List<Facility> facilities) {
        return new Enterprise(id, name, specialty, facilities);
    }

    public static List<Enterprise> provisional() {
        List<Enterprise> all = List.of(
            of("genstroi", "Кременчуггэсстрой — general contractor",
                Specialty.GENERAL_CONSTRUCTION, List.of(
                    fac("genstroi", FacilityType.HEADQUARTERS, "Construction Trust HQ", 12),
                    fac("genstroi", FacilityType.CONSTRUCTION_OFFICE, "Site construction office", 20),
                    fac("genstroi", FacilityType.STORAGE_YARD, "Trust storage yard", 6),
                    fac("genstroi", FacilityType.WORKSHOP, "Trust workshop", 15))),

            of("zemlyan", "Earthworks and Mechanisation Trust",
                Specialty.EARTHWORKS, List.of(
                    fac("zemlyan", FacilityType.HEADQUARTERS, "Earthworks Trust HQ", 10),
                    fac("zemlyan", FacilityType.CONSTRUCTION_OFFICE, "Earthworks site office", 8),
                    fac("zemlyan", FacilityType.EXCAVATION_DEPOT, "Excavation depot", 40))),

            of("zhelezobeton", "Concrete Works Trust",
                Specialty.CONCRETE_WORKS, List.of(
                    fac("zhelezobeton", FacilityType.HEADQUARTERS, "Concrete Trust HQ", 10),
                    fac("zhelezobeton", FacilityType.CONSTRUCTION_OFFICE, "Concrete works office", 8),
                    fac("zhelezobeton", FacilityType.CONCRETE_PLANT, "Concrete batching plant", 60),
                    fac("zhelezobeton", FacilityType.REBAR_PLANT, "Reinforcement plant", 80))),

            of("metallomontazh", "Steel Erection Trust",
                Specialty.STEEL_ERECTION, List.of(
                    fac("metallomontazh", FacilityType.HEADQUARTERS, "Steel Trust HQ", 8),
                    fac("metallomontazh", FacilityType.CONSTRUCTION_OFFICE, "Steelworks office", 8),
                    fac("metallomontazh", FacilityType.STEEL_YARD, "Steel fabrication yard", 50),
                    fac("metallomontazh", FacilityType.WORKSHOP, "Steel workshop", 20))),

            of("spetsraboty", "Special Works Trust",
                Specialty.SPECIAL_WORKS, List.of(
                    fac("spetsraboty", FacilityType.HEADQUARTERS, "Special Works HQ", 8),
                    fac("spetsraboty", FacilityType.CONSTRUCTION_OFFICE, "Special works office", 6),
                    fac("spetsraboty", FacilityType.PILE_DEPOT, "Pile-driving depot", 30))),

            of("elektromontazh", "Elektromontazh Trust",
                Specialty.ELECTRICAL_INSTALLATION, List.of(
                    fac("elektromontazh", FacilityType.HEADQUARTERS, "Elektromontazh HQ", 10),
                    fac("elektromontazh", FacilityType.CONSTRUCTION_OFFICE, "Elektromontazh office", 8),
                    fac("elektromontazh", FacilityType.CABLE_STORE, "Cable store", 15),
                    fac("elektromontazh", FacilityType.WORKSHOP, "Electrical workshop", 25))),

            of("teploenergomontazh", "Heat and Power Installation Trust",
                Specialty.THERMAL_INSTALLATION, List.of(
                    fac("teploenergomontazh", FacilityType.HEADQUARTERS, "Heat and Power HQ", 8),
                    fac("teploenergomontazh", FacilityType.CONSTRUCTION_OFFICE, "Heat and Power office", 8),
                    fac("teploenergomontazh", FacilityType.WORKSHOP, "Installation workshop", 30))),

            of("otdelka", "Finishing Works Trust",
                Specialty.FINISHING, List.of(
                    fac("otdelka", FacilityType.HEADQUARTERS, "Finishing Trust HQ", 6),
                    fac("otdelka", FacilityType.CONSTRUCTION_OFFICE, "Finishing office", 6),
                    fac("otdelka", FacilityType.STORAGE_YARD, "Finishing materials yard", 10))),

            of("transport", "Transport Enterprise",
                Specialty.TRANSPORT, List.of(
                    fac("transport", FacilityType.HEADQUARTERS, "Transport HQ", 6),
                    fac("transport", FacilityType.TRANSPORT_DEPOT, "Transport depot", 45))),

            of("santekhmontazh", "Sanitation and Ventilation Trust",
                Specialty.SANITATION_VENTILATION, List.of(
                    fac("santekhmontazh", FacilityType.HEADQUARTERS, "Sanitation Trust HQ", 6),
                    fac("santekhmontazh", FacilityType.CONSTRUCTION_OFFICE, "Sanitation office", 6),
                    fac("santekhmontazh", FacilityType.WORKSHOP, "Sanitation workshop", 20)))
        );
        // the general contractor is a cited fact; the specialist trusts are not
        for (Enterprise e : all) {
            e.setProvenance("genstroi".equals(e.id())
                ? Provenance.VERIFIED : Provenance.RECONSTRUCTION);
        }
        return all;
    }
}
