package com.ben.csp.planning;

/**
 * Why a planerka has been called. A planerka is not only the daily stand-up: it
 * is also where a missed goal or an accident on site is examined and people are
 * held to account, so it can be called outside the routine schedule.
 */
public enum PlanerkaReason {
    ROUTINE("Routine planerka", false),
    MILESTONE("Milestone / stage handover", false),
    ACCOUNTABILITY("Holding to account for a missed goal", true),
    EMERGENCY("Emergency (incident on site)", true);

    private final String label;
    private final boolean urgent;

    PlanerkaReason(String label, boolean urgent) {
        this.label = label;
        this.urgent = urgent;
    }

    public String label() {
        return label;
    }

    /** Urgent planerkas may be called at any hour, outside the routine slot. */
    public boolean isUrgent() {
        return urgent;
    }
}
