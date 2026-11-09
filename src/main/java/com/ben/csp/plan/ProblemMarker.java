package com.ben.csp.plan;

import java.util.Objects;

/**
 * A problem marked on the plan at a real position, with an optional выноска: a
 * callout to a separate detail sheet where the matter is drawn at a larger
 * scale. This is how "something is wrong, show me" is done on a real plan.
 */
public final class ProblemMarker {

    private final double x;
    private final double z;
    private final Severity severity;
    private final String description;
    private final String detailSheetNumber; // may be null

    public ProblemMarker(double x, double z, Severity severity, String description,
                         String detailSheetNumber) {
        this.x = x;
        this.z = z;
        this.severity = Objects.requireNonNull(severity, "severity");
        this.description = Objects.requireNonNull(description, "description");
        this.detailSheetNumber = detailSheetNumber;
    }

    public double x() { return x; }
    public double z() { return z; }
    public Severity severity() { return severity; }
    public String description() { return description; }
    public String detailSheetNumber() { return detailSheetNumber; }
    public boolean hasCallout() { return detailSheetNumber != null; }

    @Override
    public String toString() {
        return severity.label() + " at (" + x + ", " + z + "): " + description
            + (hasCallout() ? " [см. лист " + detailSheetNumber + "]" : "");
    }
}
