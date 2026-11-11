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

    /**
     * Materials needed per man-hour of work. A {@link MaterialNorm}, and by default
     * a provisional one: it has the right shape but must be replaced by a per-unit
     * figure read from the сметные нормы (СНиП IV) before it is trusted.
     */
    public static final MaterialNorm DEFAULT_MATERIAL_NORM = MaterialNorm.provisional(
        0.05, "СНиП IV сметные нормы: replace with per-unit consumption");

    private final String id;
    private final String facilityId;
    private final Footprint plot;
    private final double requiredManHours;
    private final double materialsRequired;
    private final MaterialNorm materialNorm;

    private String brigadeId;
    private double manHoursDone;
    private double materialsOnSite;

    public ConstructionSite(String id, String facilityId, Footprint plot, double requiredManHours) {
        this(id, facilityId, plot, requiredManHours, DEFAULT_MATERIAL_NORM);
    }

    public ConstructionSite(String id, String facilityId, Footprint plot,
                            double requiredManHours, MaterialNorm materialNorm) {
        this.id = Objects.requireNonNull(id, "id");
        this.facilityId = Objects.requireNonNull(facilityId, "facilityId");
        this.plot = Objects.requireNonNull(plot, "plot");
        this.materialNorm = Objects.requireNonNull(materialNorm, "materialNorm");
        if (requiredManHours <= 0) {
            throw new IllegalArgumentException("required man-hours must be positive");
        }
        this.requiredManHours = requiredManHours;
        this.materialsRequired = materialNorm.forManHours(requiredManHours);
    }

    /** The material norm this site is planned against. */
    public MaterialNorm materialNorm() {
        return materialNorm;
    }

    /**
     * The site's material demand broken down by kind, so a delivery plan can name
     * what it needs. The split is the provisional shape of a reinforced-concrete
     * facility (mostly concrete and reinforcement); it becomes sourced when the
     * сметные нормы are read in.
     */
    public java.util.Map<MaterialKind, Double> materialBreakdown() {
        double m = materialsRequired;
        java.util.Map<MaterialKind, Double> out = new java.util.EnumMap<>(MaterialKind.class);
        out.put(MaterialKind.CONCRETE, m * 0.55);
        out.put(MaterialKind.REINFORCEMENT_STEEL, m * 0.15);
        out.put(MaterialKind.STEEL_SECTIONS, m * 0.10);
        out.put(MaterialKind.BRICK, m * 0.10);
        out.put(MaterialKind.BALLAST, m * 0.10);
        return out;
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
        if (brigadeId == null || manHours <= 0) {
            return 0;
        }
        double rate = materialNorm.unitsPerManHour();
        if (rate > 0 && materialsOnSite <= 0) {
            return 0;
        }
        double remaining = requiredManHours - manHoursDone;
        double byMaterials = rate > 0 ? materialsOnSite / rate : Double.MAX_VALUE;
        double applied = Math.min(manHours, Math.min(remaining, byMaterials));
        if (applied <= 0) {
            return 0;
        }
        manHoursDone += applied;
        materialsOnSite = Math.max(0.0, materialsOnSite - applied * rate);
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
