package com.ben.csp.survey;

import com.ben.csp.prombaza.Area;

import java.util.Objects;

/**
 * A geodesist's survey of a piece of ground, carried out over time on the work
 * clock. It is not instant: the surveyor walks the site and the plan is built up
 * as the work proceeds. When enough work has been done the survey is complete
 * and yields a proposed site — which becomes the request to designate a
 * prombaza territory.
 */
public final class Survey {

    public enum Status { IN_PROGRESS, COMPLETE }

    private final String id;
    private final String geodezistId;
    private final Area area;
    private final String proposedName;
    private final long startDay;
    private final double requiredWorkHours;

    private double doneWorkHours;
    private Status status = Status.IN_PROGRESS;
    private long completedDay = -1;

    public Survey(String id, String geodezistId, Area area, String proposedName,
                  long startDay, double requiredWorkHours) {
        this.id = Objects.requireNonNull(id, "id");
        this.geodezistId = Objects.requireNonNull(geodezistId, "geodezistId");
        this.area = Objects.requireNonNull(area, "area");
        this.proposedName = Objects.requireNonNull(proposedName, "proposedName");
        this.startDay = startDay;
        if (requiredWorkHours <= 0) {
            throw new IllegalArgumentException("a survey must require some work");
        }
        this.requiredWorkHours = requiredWorkHours;
    }

    public String id() { return id; }
    public String geodezistId() { return geodezistId; }
    public Area area() { return area; }
    public String proposedName() { return proposedName; }
    public long startDay() { return startDay; }
    public double requiredWorkHours() { return requiredWorkHours; }
    public double doneWorkHours() { return doneWorkHours; }
    public Status status() { return status; }
    public long completedDay() { return completedDay; }
    public boolean isComplete() { return status == Status.COMPLETE; }

    /** Fraction of the survey done, 0..1. This is what the plan shows filling in. */
    public double progress() {
        return Math.min(1.0, doneWorkHours / requiredWorkHours);
    }

    /**
     * Advance the survey by work-hours. Only a complete survey stops accepting
     * work; there is no way to rush it beyond doing the hours.
     */
    public void work(double hours, long day) {
        if (status == Status.COMPLETE) {
            throw new IllegalStateException("the survey is already complete");
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
        return "Survey#" + id + " of " + area.describe()
            + " by " + geodezistId + " — " + Math.round(progress() * 100) + "% " + status;
    }
}
