package com.ben.csp.plan;

/** How serious a marked problem on the plan is. */
public enum Severity {
    NOTE("Note"),
    WARNING("Warning"),
    PROBLEM("Problem");

    private final String label;

    Severity(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
