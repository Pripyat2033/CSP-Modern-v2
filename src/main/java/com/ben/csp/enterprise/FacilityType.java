package com.ben.csp.enterprise;

/**
 * A production or support facility an enterprise must stand up inside the
 * prombaza before it can do its work. An enterprise is not one building: it is
 * a chain of them (for example a construction trust cannot pour concrete until
 * it has built its own batching plant and reinforcement yard).
 */
public enum FacilityType {
    SITE_OFFICE("Site office", true),
    STORAGE_YARD("Storage yard", true),
    WORKSHOP("Workshop", true),
    CONCRETE_PLANT("Concrete batching plant", false),
    REBAR_YARD("Reinforcement yard", false),
    EXCAVATION_FLEET("Excavation fleet", false),
    STEEL_YARD("Steel fabrication yard", false),
    CABLE_STORE("Cable store", false),
    TRANSPORT_DEPOT("Transport depot", false),
    PILE_RIG("Pile-driving rig", false);

    private final String label;
    private final boolean support;

    FacilityType(String label, boolean support) {
        this.label = label;
        this.support = support;
    }

    public String label() {
        return label;
    }

    /** Support facilities are stood up first; they serve the production ones. */
    public boolean isSupport() {
        return support;
    }
}
