package com.ben.csp.zone;

/**
 * The kinds of material a Soviet construction site consumed. Used to break a
 * site's total material demand into what it is actually made of, so a delivery
 * plan can name concrete and reinforcement rather than "materials".
 */
public enum MaterialKind {
    CONCRETE("concrete"),
    REINFORCEMENT_STEEL("reinforcement steel"),
    BRICK("brick"),
    TIMBER("timber"),
    STEEL_SECTIONS("steel sections"),
    CABLE("cable"),
    BALLAST("ballast / aggregate");

    private final String label;

    MaterialKind(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
