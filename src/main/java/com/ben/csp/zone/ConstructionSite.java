package com.ben.csp.zone;

import com.ben.csp.enterprise.Footprint;

import java.util.Objects;

/**
 * A facility being built. It is set out as a marker, and then grows into a
 * building as a brigade is assigned to it and materials are delivered to it. It
 * makes progress only while it has both a brigade and materials on site; strip it
 * of either and it stands still.
 */
public final class ConstructionSite {

    /** Materials needed per man-hour of work. A placeholder pending sourced norms. */
    private static final double MATERIALS_PER_MAN_HOUR = 0.05;

    private final String id;
    private final String facilityId;
    private final Footprint plot;
    private final double requiredManHours;
    private final double materialsRequired;

    private String brigadeId;
    private double manHoursDone;
    private double materialsOnSite;

    public ConstructionSite(String id, String facilityId, Footprint plot, double requiredManHours) {
        this.id = Objects.requireNonNull(id, "id");
        this.facilityId = Objects.requireNonNull(facilityId, "facilityId");
        this.plot = Objects.requireNonNull(plot, "plot");
        if (requiredManHours <= 0) {
            throw new IllegalArgumentException("required man-hours must be positive");
        }
        this.requiredManHours = requiredManHours;
        this.materialsRequired = requiredManHours * MATERIALS_PER_MAN_HOUR;
    }

    public String id() { return id; }
    public String facilityId() { return facilityId; }
    public Footprint plot() { return plot; }
    public double requiredManHours() { return requiredManHours; }
    public double manHoursDone() { return manHoursDone; }
    public double materialsRequired() { return materialsRequired; }
    public double materialsOnSite() { return materialsOnSite; }
    public String brigadeId() { return brigadeId; }
    public boolean hasBrigade() { return brigadeId != null; }
    public boolean hasMaterials() { return materialsOnSite > 0; }

    /** Restore a site's progress from a save. */
    public void restore(double manHoursDone, double materialsOnSite, String brigadeId) {
        this.manHoursDone = Math.max(0, Math.min(requiredManHours, manHoursDone));
        this.materialsOnSite = Math.max(0, materialsOnSite);
        this.brigadeId = brigadeId;
    }

    /** The construction office assigns a brigade to this site. */
    public void assignBrigade(String brigadeId) {
        this.brigadeId = Objects.requireNonNull(brigadeId, "brigadeId");
    }

    /** The construction office delivers materials to this site. */
    public void deliverMaterials(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("materials must not be negative");
        }
        this.materialsOnSite += amount;
    }

    /** How far the build has gone, 0..1. */
    public double progress() {
        return manHoursDone / requiredManHours;
    }

    public boolean isComplete() {
        return manHoursDone >= requiredManHours;
    }

    /**
     * Put a day's labour in. Returns the man-hours actually done: zero if there is
     * no brigade or no materials, and otherwise capped by what the materials allow.
     */
    public double work(double manHours) {
        if (brigadeId == null || materialsOnSite <= 0 || manHours <= 0) {
            return 0;
        }
        double remaining = requiredManHours - manHoursDone;
        double byMaterials = materialsOnSite / MATERIALS_PER_MAN_HOUR;
        double applied = Math.min(manHours, Math.min(remaining, byMaterials));
        if (applied <= 0) {
            return 0;
        }
        manHoursDone += applied;
        materialsOnSite = Math.max(0.0, materialsOnSite - applied * MATERIALS_PER_MAN_HOUR);
        return applied;
    }

    /** The stage the site has reached. */
    public BuildStage stage() {
        if (isComplete()) {
            return BuildStage.FINISHED;
        }
        double p = progress();
        if (p >= 0.80) {
            return BuildStage.ROOFED;
        }
        if (p >= 0.45) {
            return BuildStage.STRUCTURE;
        }
        if (p >= 0.15) {
            return BuildStage.FOUNDATION;
        }
        return BuildStage.STAKED;
    }

    @Override
    public String toString() {
        return "site[" + facilityId + "] " + stage() + " "
            + Math.round(progress() * 100) + "%" + (hasBrigade() ? "" : " (no brigade)");
    }
}
