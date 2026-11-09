package com.ben.csp.enterprise;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * One real industrial building inside the zone, run by its own facility director.
 *
 * <p>A facility is not a marker: it is a building that has to be constructed,
 * given a director, and manned. It has a production quota to meet and needs
 * supplies to run; accidents on it have to be answered for. Every facility the
 * enterprise builds must have a facility director, and that director reports to
 * the enterprise director.
 */
public final class Facility {

    private final String id;
    private final FacilityType type;
    private final String name;
    private final int requiredStaff;

    private boolean built;
    private String facilityDirectorId;
    private int staff;

    private double quotaPerDay;
    private double supplies;
    private double supplyPerUnit = 0.0; // 0 means this facility needs no tracked supplies

    private double produced;        // lifetime output
    private double producedToday;
    private Footprint footprint;    // where the building stands, once sited
    private Provenance provenance = Provenance.RECONSTRUCTION; // verified, or reconstructed
    private boolean asBuiltAccepted; // signed off by the geodezist after the build
    private boolean asBuiltRefused;  // failed the check and is not signed off
    private final List<Incident> incidents = new ArrayList<>();

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
    public String facilityDirectorId() { return facilityDirectorId; }
    public int staff() { return staff; }
    public int requiredStaff() { return requiredStaff; }
    public double quotaPerDay() { return quotaPerDay; }
    public double supplies() { return supplies; }
    public double produced() { return produced; }
    public double producedToday() { return producedToday; }

    /** Where the building stands, or null if it has not been sited yet. */
    public Footprint footprint() { return footprint; }
    public boolean hasFootprint() { return footprint != null; }

    /** Whether this facility is a cited fact or a reconstruction. */
    public Provenance provenance() { return provenance; }

    /** Mark this facility as taken from a source, or as a reconstruction. */
    public void setProvenance(Provenance provenance) {
        this.provenance = Objects.requireNonNull(provenance, "provenance");
    }

    /** Site the facility at its real place on the ground. */
    public void site(Footprint footprint) {
        this.footprint = Objects.requireNonNull(footprint, "footprint");
    }

    /** True once the geodezist has checked the finished building and signed it off. */
    public boolean isAsBuiltAccepted() { return asBuiltAccepted; }

    /** True once the geodezist has failed the building and refused to sign it off. */
    public boolean isAsBuiltRefused() { return asBuiltRefused; }

    /** Sign the building off after the as-built check has passed. */
    public void acceptAsBuilt() {
        if (!built) {
            throw new IllegalStateException("the as-built check needs a finished building");
        }
        this.asBuiltAccepted = true;
        this.asBuiltRefused = false;
    }

    /** Refuse to sign the building off after a failed as-built check. */
    public void refuseAsBuilt() {
        if (!built) {
            throw new IllegalStateException("the as-built check needs a finished building");
        }
        this.asBuiltRefused = true;
        this.asBuiltAccepted = false;
    }

    public void completeConstruction() {
        if (footprint == null) {
            throw new IllegalStateException(
                "a facility cannot be built before a geodezist has set it out on the ground");
        }
        this.built = true;
    }

    /** Every facility has a facility director; without one it cannot work. */
    public void assignFacilityDirector(String directorId) {
        this.facilityDirectorId = Objects.requireNonNull(directorId, "directorId");
    }

    public void setStaff(int staff) {
        if (staff < 0) {
            throw new IllegalArgumentException("staff must not be negative");
        }
        this.staff = staff;
    }

    public void setQuotaPerDay(double quotaPerDay) {
        if (quotaPerDay < 0) {
            throw new IllegalArgumentException("quota must not be negative");
        }
        this.quotaPerDay = quotaPerDay;
    }

    public void setSupplyPerUnit(double supplyPerUnit) {
        if (supplyPerUnit < 0) {
            throw new IllegalArgumentException("supply per unit must not be negative");
        }
        this.supplyPerUnit = supplyPerUnit;
    }

    public void deliverSupplies(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("supplies must not be negative");
        }
        this.supplies += amount;
    }

    /** A facility works only once built, directed and manned. */
    public boolean isOperational() {
        return built && facilityDirectorId != null && staff >= requiredStaff;
    }

    /**
     * Advance production by work-hours. Output is limited by staff and by the
     * supplies on hand; running dry of supplies is exactly the kind of problem a
     * facility director has to raise.
     */
    public void work(double workHours) {
        if (!isOperational() || workHours <= 0) {
            return;
        }
        double effort = Math.min(1.0, (double) staff / Math.max(1, requiredStaff));
        double potential = workHours * effort;
        if (supplyPerUnit > 0) {
            potential = Math.min(potential, supplies / supplyPerUnit);
        }
        if (potential <= 0) {
            return;
        }
        produced += potential;
        producedToday += potential;
        supplies = Math.max(0.0, supplies - potential * supplyPerUnit);
    }

    public boolean isMeetingQuota() {
        return quotaPerDay <= 0 || producedToday + 1e-9 >= quotaPerDay;
    }

    /** Called at each work-day boundary; reports whether yesterday's quota was met. */
    public boolean startNewDay() {
        boolean met = isMeetingQuota();
        producedToday = 0;
        return met;
    }

    public void recordIncident(String description, long day) {
        incidents.add(new Incident(description, day));
    }

    public List<Incident> unresolvedIncidents() {
        List<Incident> open = new ArrayList<>();
        for (Incident i : incidents) {
            if (!i.isResolved()) {
                open.add(i);
            }
        }
        return open;
    }

    public List<Incident> incidents() {
        return Collections.unmodifiableList(incidents);
    }

    public boolean hasUnresolvedIncidents() {
        return incidents.stream().anyMatch(i -> !i.isResolved());
    }

    @Override
    public String toString() {
        return name + " (" + type.label() + ") " + (isOperational() ? "operational"
            : built ? "built, not manned" : "not built");
    }
}
