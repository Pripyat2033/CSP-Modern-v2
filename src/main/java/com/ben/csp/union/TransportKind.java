package com.ben.csp.union;

/** How the zone is linked to the rest of the country. */
public enum TransportKind {
    RAIL("rail"),
    ROAD("road"),
    RIVER("river");

    private final String label;

    TransportKind(String label) { this.label = label; }

    public String label() { return label; }
}
