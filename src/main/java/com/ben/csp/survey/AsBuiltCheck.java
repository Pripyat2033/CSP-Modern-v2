package com.ben.csp.survey;

import com.ben.csp.enterprise.Footprint;
import com.ben.csp.plan.ProblemMarker;
import com.ben.csp.plan.Severity;

import java.util.Objects;
import java.util.Optional;

/**
 * The as-built check (исполнительная съёмка) of one building: after it is built,
 * a geodezist measures where it actually stands and compares that with the
 * design. If it is within tolerance the building passes and is signed off; if
 * not, it fails and has to be put right. Like every geodetic job it takes time
 * on the work clock.
 */
public final class AsBuiltCheck {

    public enum Status { IN_PROGRESS, PASSED, FAILED }

    private final String id;
    private final String geodezistId;
    private final String facilityId;
    private final Footprint design;
    private final Footprint asBuilt;
    private final double tolerance;
    private final long startDay;
    private final double requiredWorkHours;

    private double doneWorkHours;
    private Status status = Status.IN_PROGRESS;
    private long completedDay = -1;

    public AsBuiltCheck(String id, String geodezistId, String facilityId,
                        Footprint design, Footprint asBuilt, double tolerance,
                        long startDay, double requiredWorkHours) {
        this.id = Objects.requireNonNull(id, "id");
        this.geodezistId = Objects.requireNonNull(geodezistId, "geodezistId");
        this.facilityId = Objects.requireNonNull(facilityId, "facilityId");
        this.design = Objects.requireNonNull(design, "design");
        this.asBuilt = Objects.requireNonNull(asBuilt, "asBuilt");
        if (tolerance < 0) {
            throw new IllegalArgumentException("tolerance must not be negative");
        }
        this.tolerance = tolerance;
        this.startDay = startDay;
        if (requiredWorkHours <= 0) {
            throw new IllegalArgumentException("a check must require some work");
        }
        this.requiredWorkHours = requiredWorkHours;
    }

    public String id() { return id; }
    public String geodezistId() { return geodezistId; }
    public String facilityId() { return facilityId; }
    public Footprint design() { return design; }
    public Footprint asBuilt() { return asBuilt; }
    public double tolerance() { return tolerance; }
    public long startDay() { return startDay; }
    public double requiredWorkHours() { return requiredWorkHours; }
    public double doneWorkHours() { return doneWorkHours; }
    public Status status() { return status; }
    public long completedDay() { return completedDay; }
    public boolean isComplete() { return status != Status.IN_PROGRESS; }
    public boolean passed() { return status == Status.PASSED; }
    public boolean failed() { return status == Status.FAILED; }

    public double progress() {
        return Math.min(1.0, doneWorkHours / requiredWorkHours);
    }

    /** The largest error, in metres, between where it should stand and where it does. */
    public double deviation() {
        return Math.max(Math.max(Math.abs(design.minX() - asBuilt.minX()),
                                 Math.abs(design.maxX() - asBuilt.maxX())),
                        Math.max(Math.abs(design.minZ() - asBuilt.minZ()),
                                 Math.abs(design.maxZ() - asBuilt.maxZ())));
    }

    /** Whether the building stands within tolerance. Meaningful once complete. */
    public boolean isWithinTolerance() {
        return deviation() <= tolerance;
    }

    /**
     * When the check has failed, the marker to put on the plan: it flags the
     * building at its as-built position with the measured deviation, and carries
     * a выноска to a detail sheet. A structure that is off the design is both
     * refused and flagged, never quietly passed.
     */
    public Optional<ProblemMarker> failureMarker(String detailSheetNumber) {
        if (status != Status.FAILED) {
            return Optional.empty();
        }
        String text = String.format("As-built deviation %.2f m (limit %.2f m): %s",
            deviation(), tolerance, facilityId);
        return Optional.of(new ProblemMarker(asBuilt.centerX(), asBuilt.centerZ(),
            Severity.PROBLEM, text, detailSheetNumber));
    }

    public void work(double hours, long day) {
        if (status != Status.IN_PROGRESS) {
            throw new IllegalStateException("the check is already complete");
        }
        if (hours < 0) {
            throw new IllegalArgumentException("work hours must not be negative");
        }
        doneWorkHours += hours;
        if (doneWorkHours >= requiredWorkHours) {
            status = isWithinTolerance() ? Status.PASSED : Status.FAILED;
            completedDay = day;
        }
    }

    @Override
    public String toString() {
        return "AsBuiltCheck#" + id + " of " + facilityId + " — "
            + Math.round(progress() * 100) + "% " + status
            + " (deviation " + String.format("%.2f", deviation()) + " m)";
    }
}
