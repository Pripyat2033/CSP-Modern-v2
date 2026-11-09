package com.ben.csp.enterprise;

/** The trade an enterprise specialises in (a Soviet specialised trust). */
public enum Specialty {
    GENERAL_CONSTRUCTION("General construction"),
    EARTHWORKS("Earthworks and mechanisation"),
    CONCRETE_WORKS("Concrete and reinforced concrete"),
    STEEL_ERECTION("Steel erection"),
    SPECIAL_WORKS("Special works (piling, dewatering)"),
    ELECTRICAL_INSTALLATION("Electrical installation (Elektromontazh)"),
    THERMAL_INSTALLATION("Heat and power installation"),
    FINISHING("Finishing works"),
    TRANSPORT("Transport and haulage"),
    SANITATION_VENTILATION("Sanitation and ventilation");

    private final String label;

    Specialty(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
