package com.ben.csp.enterprise;

import java.util.Objects;

/**
 * One real industrial building inside the zone.
 *
 * <p>A facility is not a marker: it is a building that has to be constructed,
 * staffed, and put to work. It has its own facility director, its own staff, and
 * its own production process. Until it is built, directed and manned it does
 * nothing; once it is, its process advances with the work clock.
 */
public final class Facility {

    private final String id;
    private final FacilityType type;
    private final String name;

    private boolean built;
    private String directorId;      // its own facility director NPC
    private int staff;
    private final int requiredStaff;
    private double produced;        // units of product accumulated

    public Facility(String id, FacilityType type, String name, int requiredStaff) {
        this.id = Objects.requireNonNull(id, "id");
        this.type = Objects.requireNonNull(type, "type");
        this.name = Objects.requireNonNull(name, "name");
        if (requiredStaff < 0) {
            throw new IllegalArgumentException("required staff must not be negative");
        }
        this.requiredStaff = requiredStaff;
    }

    public String id() { return id; }
    public FacilityType type() { return type; }
    public String name() { return name; }
    public boolean isBuilt() { return built; }
    public String directorId() { return directorId; }
    public int staff() { return staff; }
    public int requiredStaff() { return requiredStaff; }
    public double produced() { return produced; }

    public void completeConstruction() {
        this.built = true;
    }

    public void assignDirector(String directorId) {
        this.directorId = Objects.requireNonNull(directorId, "directorId");
    }

    public void setStaff(int staff) {
        if (staff < 0) {
            throw new IllegalArgumentException("staff must not be negative");
        }
        this.staff = staff;
    }

    /** A facility works only once built, directed and manned. */
    public boolean isOperational() {
        return built && directorId != null && staff >= requiredStaff;
    }

    /**
     * Advance this facility's production by a number of work-hours. Only an
     * operational facility produces; output scales with staff against the
     * requirement, capped at 1.0 (no over-production from extra hands).
     */
    public void work(double workHours) {
        if (!isOperational() || workHours <= 0) {
            return;
        }
        double effort = Math.min(1.0, (double) staff / Math.max(1, requiredStaff));
        produced += workHours * effort;
    }

    @Override
    public String toString() {
        return name + " (" + type.label() + ") " + (isOperational() ? "operational"
            : built ? "built, not manned" : "not built");
    }
}
