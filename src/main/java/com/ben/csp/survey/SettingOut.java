package com.ben.csp.survey;

import com.ben.csp.enterprise.Footprint;

import java.util.Objects;

/**
 * The setting-out (разбивка) of one building: a geodezist marks the building's
 * axes on the ground from the design, so the builders know exactly where to dig
 * and pour. No building starts without it. It takes time on the work clock, and
 * when it is done it fixes the building's footprint — the facility is sited.
 */
public final class SettingOut {

    public enum Status { IN_PROGRESS, COMPLETE }

    private final String id;
    private final String geodezistId;
    private final String facilityId;
    private final Footprint design;
    private final long startDay;
    private final double requiredWorkHours;

    private double doneWorkHours;
    private Status status = Status.IN_PROGRESS;
    private long completedDay = -1;

    public SettingOut(String id, String geodezistId, String facilityId,
                      Footprint design, long startDay, double requiredWorkHours) {
        this.id = Objects.requireNonNull(id, "id");
        this.geodezistId = Objects.requireNonNull(geodezistId, "geodezistId");
        this.facilityId = Objects.requireNonNull(facilityId, "facilityId");
        this.design = Objects.requireNonNull(design, "design");
        this.startDay = startDay;
        if (requiredWorkHours <= 0) {
            throw new IllegalArgumentException("a setting-out must require some work");
        }
        this.requiredWorkHours = requiredWorkHours;
    }

    public String id() { return id; }
    public String geodezistId() { return geodezistId; }
    public String facilityId() { return facilityId; }
    public Footprint design() { return design; }
    public long startDay() { return startDay; }
    public double requiredWorkHours() { return requiredWorkHours; }
    public double doneWorkHours() { return doneWorkHours; }
    public Status status() { return status; }
    public long completedDay() { return completedDay; }
    public boolean isComplete() { return status == Status.COMPLETE; }

    public double progress() {
        return Math.min(1.0, doneWorkHours / requiredWorkHours);
    }

    /** The footprint the building is set out at. Only valid once complete. */
    public Footprint establishedFootprint() {
        if (status != Status.COMPLETE) {
            throw new IllegalStateException("the setting-out is not complete");
        }
        return design;
    }

    public void work(double hours, long day) {
        if (status == Status.COMPLETE) {
            throw new IllegalStateException("the setting-out is already complete");
        }
        if (hours < 0) {
            throw new IllegalArgumentException("work hours must not be negative");
        }
        doneWorkHours += hours;
        if (doneWorkHours >= requiredWorkHours) {
            status = Status.COMPLETE;
            completedDay = day;
        }
    }

    @Override
    public String toString() {
        return "SettingOut#" + id + " of " + facilityId + " by " + geodezistId
            + " — " + Math.round(progress() * 100) + "% " + status;
    }
}
