package com.ben.csp.infra;

/**
 * The kind of connection between facilities. The kind decides which specialist
 * sheet the route is drawn on: roads and rail go on the transport sheet, the
 * buried and cabled services on the utilities sheet.
 */
public enum RouteKind {
    ROAD("Road", SheetGroup.TRANSPORT),
    RAIL("Rail", SheetGroup.TRANSPORT),
    PIPE("Pipe", SheetGroup.UTILITIES),
    CABLE("Cable", SheetGroup.UTILITIES),
    WATER("Water main", SheetGroup.UTILITIES),
    SEWER("Sewer", SheetGroup.UTILITIES),
    POWER_LINE("Power line", SheetGroup.UTILITIES);

    private final String label;
    private final SheetGroup group;

    RouteKind(String label, SheetGroup group) {
        this.label = label;
        this.group = group;
    }

    public String label() { return label; }
    public SheetGroup group() { return group; }
}
