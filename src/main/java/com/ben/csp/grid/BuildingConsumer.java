package com.ben.csp.grid;

import java.util.Objects;

/** A simple consumer: a functional building on the map that draws a fixed load. */
public final class BuildingConsumer implements PowerConsumer {

    private final String id;
    private final String name;
    private final double demandMw;
    private final LoadPriority priority;
    private boolean energised = true;

    public BuildingConsumer(String id, String name, double demandMw, LoadPriority priority) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        if (demandMw < 0) {
            throw new IllegalArgumentException("demand must not be negative");
        }
        this.demandMw = demandMw;
        this.priority = Objects.requireNonNull(priority, "priority");
    }

    @Override public String id() { return id; }
    public String name() { return name; }
    @Override public double demandMw() { return demandMw; }
    @Override public LoadPriority priority() { return priority; }
    @Override public boolean isEnergised() { return energised; }
    @Override public void setEnergised(boolean energised) { this.energised = energised; }

    @Override public String toString() {
        return name + " " + demandMw + " MW [" + priority.label() + "]"
            + (energised ? "" : " (shed)");
    }
}
