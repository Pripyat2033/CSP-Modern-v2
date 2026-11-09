package com.ben.csp.prombaza;

/**
 * The kinds of temporary facility that stand on a construction site plan
 * (стройгенплан): the offices, stores and services a building site needs while
 * it is building, and which are taken down when the job is done.
 */
public enum SiteFacilityKind {
    OFFICE("office"),
    ARCHIVE("records archive"),
    WAREHOUSE("warehouse"),
    CANTEEN("canteen"),
    WORKSHOP("workshop"),
    RAILHEAD("railhead"),
    TRUCK_PARK("truck park"),
    CRANE_TRACK("crane track"),
    ROAD("site road"),
    GATE("site gate"),
    GUARDHOUSE("guardhouse");

    private final String label;

    SiteFacilityKind(String label) { this.label = label; }

    public String label() { return label; }
}
