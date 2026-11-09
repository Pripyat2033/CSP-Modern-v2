package com.ben.csp.directorate;

/** The kinds of decision that come to the director for approval. */
public enum RequestType {
    PROMBAZA_DESIGNATION("Designate a prombaza territory"),
    FACILITY_CONSTRUCTION("Build an enterprise facility"),
    FACILITY_REPAIR("Repair an enterprise facility");

    private final String label;

    RequestType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
