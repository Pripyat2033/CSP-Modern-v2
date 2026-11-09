package com.ben.csp.enterprise;

/**
 * The kind of industrial building a facility is. Every one of these is a real,
 * physical building that must be constructed and staffed in the zone; none is a
 * marker on a map.
 */
public enum FacilityType {
    HEADQUARTERS("Enterprise headquarters", true),
    CONSTRUCTION_OFFICE("Construction office", true),
    STORAGE_YARD("Storage yard", true),
    WORKSHOP("Workshop", true),
    CONCRETE_PLANT("Concrete batching plant", false),
    REBAR_PLANT("Reinforcement plant", false),
    EXCAVATION_DEPOT("Excavation depot", false),
    STEEL_YARD("Steel fabrication yard", false),
    CABLE_STORE("Cable store", false),
    TRANSPORT_DEPOT("Transport depot", false),
    PILE_DEPOT("Pile-driving depot", false);

    private final String label;
    private final boolean support;

    FacilityType(String label, boolean support) {
        this.label = label;
        this.support = support;
    }

    public String label() {
        return label;
    }

    /** Support buildings are stood up first; they serve the production ones. */
    public boolean isSupport() {
        return support;
    }
}
