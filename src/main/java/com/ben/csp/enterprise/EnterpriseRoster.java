package com.ben.csp.enterprise;

import java.util.List;

/**
 * The enterprises that build the zone.
 *
 * <p>IMPORTANT: these are PROVISIONAL names. The specialities are the historical
 * categories of Soviet construction trusts, but the exact organisations that
 * built ChNPP have not been verified against a source. Rename freely once the
 * real names are known; only the ids should stay stable.
 */
public final class EnterpriseRoster {

    private EnterpriseRoster() {}

    public static List<Enterprise> provisional() {
        return List.of(
            new Enterprise("genstroi", "Construction Trust (general contractor)",
                Specialty.GENERAL_CONSTRUCTION,
                List.of(FacilityType.SITE_OFFICE, FacilityType.STORAGE_YARD, FacilityType.WORKSHOP)),

            new Enterprise("zemlyan", "Earthworks and Mechanisation Trust",
                Specialty.EARTHWORKS,
                List.of(FacilityType.SITE_OFFICE, FacilityType.STORAGE_YARD,
                        FacilityType.EXCAVATION_FLEET)),

            new Enterprise("zhelezobeton", "Concrete Works Trust",
                Specialty.CONCRETE_WORKS,
                List.of(FacilityType.SITE_OFFICE, FacilityType.STORAGE_YARD,
                        FacilityType.CONCRETE_PLANT, FacilityType.REBAR_YARD)),

            new Enterprise("metallomontazh", "Steel Erection Trust",
                Specialty.STEEL_ERECTION,
                List.of(FacilityType.SITE_OFFICE, FacilityType.STEEL_YARD,
                        FacilityType.WORKSHOP)),

            new Enterprise("spetsraboty", "Special Works Trust",
                Specialty.SPECIAL_WORKS,
                List.of(FacilityType.SITE_OFFICE, FacilityType.PILE_RIG, FacilityType.WORKSHOP)),

            new Enterprise("elektromontazh", "Elektromontazh Trust",
                Specialty.ELECTRICAL_INSTALLATION,
                List.of(FacilityType.SITE_OFFICE, FacilityType.WORKSHOP, FacilityType.CABLE_STORE)),

            new Enterprise("teploenergomontazh", "Heat and Power Installation Trust",
                Specialty.THERMAL_INSTALLATION,
                List.of(FacilityType.SITE_OFFICE, FacilityType.WORKSHOP, FacilityType.STORAGE_YARD)),

            new Enterprise("otdelka", "Finishing Works Trust",
                Specialty.FINISHING,
                List.of(FacilityType.SITE_OFFICE, FacilityType.STORAGE_YARD, FacilityType.WORKSHOP)),

            new Enterprise("transport", "Transport Enterprise",
                Specialty.TRANSPORT,
                List.of(FacilityType.SITE_OFFICE, FacilityType.TRANSPORT_DEPOT)),

            new Enterprise("santekhmontazh", "Sanitation and Ventilation Trust",
                Specialty.SANITATION_VENTILATION,
                List.of(FacilityType.SITE_OFFICE, FacilityType.WORKSHOP, FacilityType.STORAGE_YARD))
        );
    }
}
