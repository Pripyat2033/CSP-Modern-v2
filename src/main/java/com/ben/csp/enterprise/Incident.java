package com.ben.csp.enterprise;

import java.util.Objects;

/**
 * An accident or fault at a facility that has to be answered for. Recording and
 * clearing these is the facility director's work; an unresolved incident is a
 * standing problem on his watch.
 */
public final class Incident {

    private final String description;
    private final long day;
    private boolean resolved;

    public Incident(String description, long day) {
        this.description = Objects.requireNonNull(description, "description");
        this.day = day;
    }

    public String description() { return description; }
    public long day() { return day; }
    public boolean isResolved() { return resolved; }

    public void resolve() {
        this.resolved = true;
    }

    @Override
    public String toString() {
        return (resolved ? "[resolved] " : "[open] ") + "day " + day + ": " + description;
    }
}
