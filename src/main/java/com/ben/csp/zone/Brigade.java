package com.ben.csp.zone;

import com.ben.csp.enterprise.Specialty;

import java.util.Objects;

/**
 * A brigade: a gang of workers of one trade, with a daily labour capacity. The
 * enterprise's construction office assigns a brigade to a facility, and the
 * facility grows by however much the brigade can do that day.
 */
public final class Brigade {

    private final String id;
    private final String name;
    private final Specialty trade;
    private final int workers;
    private final double hoursPerDay;

    public Brigade(String id, String name, Specialty trade, int workers, double hoursPerDay) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.trade = Objects.requireNonNull(trade, "trade");
        if (workers < 0 || hoursPerDay < 0) {
            throw new IllegalArgumentException("workers and hours must not be negative");
        }
        this.workers = workers;
        this.hoursPerDay = hoursPerDay;
    }

    public String id() { return id; }
    public String name() { return name; }
    public Specialty trade() { return trade; }
    public int workers() { return workers; }
    public double hoursPerDay() { return hoursPerDay; }

    /** The labour this brigade puts in on one working day, in man-hours. */
    public double dailyManHours() {
        return workers * hoursPerDay;
    }

    @Override
    public String toString() {
        return name + " (" + workers + " " + trade.label() + ")";
    }
}
