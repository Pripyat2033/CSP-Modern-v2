package com.ben.csp.grid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A busbar (секция шин) at one voltage. Loads hang off it, sources feed it, and
 * a bus is live only if a source is actually connected through closed breakers.
 *
 * <p>This is the missing link in the plant model. Before it, the pumps had a
 * {@code running} flag that a chat command could flip, so a pump could keep
 * turning with no electricity anywhere. Now the pump's motor is a load on a 6 kV
 * section: when the section is dead the pump is unpowered, and the only thing
 * keeping coolant moving is its flywheel.
 */
public final class ElectricalBus {

    private final String id;
    private final String name;
    /** Nominal voltage, kV. */
    private final double voltageKv;
    private final List<PlantLoad> loads = new ArrayList<>();
    private final List<Breaker> incomers = new ArrayList<>();
    /** True when a source is connected through a closed incomer. */
    private boolean live;

    public ElectricalBus(String id, String name, double voltageKv) {
        this.id = id;
        this.name = name;
        this.voltageKv = voltageKv;
    }

    public String id() { return id; }
    public String name() { return name; }
    public double voltageKv() { return voltageKv; }
    public boolean isLive() { return live; }

    /** A breaker between a source and this bus. */
    public Breaker addIncomer(String breakerId, String breakerName) {
        Breaker b = new Breaker(breakerId, breakerName);
        incomers.add(b);
        return b;
    }

    public List<Breaker> incomers() {
        return Collections.unmodifiableList(incomers);
    }

    /** True if any incomer is closed. Voltage itself is decided by the network. */
    public boolean hasClosedIncomer() {
        return incomers.stream().anyMatch(Breaker::isClosed);
    }

    public void addLoad(PlantLoad load) {
        loads.add(load);
    }

    public List<PlantLoad> loads() {
        return Collections.unmodifiableList(loads);
    }

    /**
     * Set whether this bus is live and push that to every load on it. Loads are
     * told even when the state does not change, because a load's own logic may
     * depend on being re-energised (a pump motor restarts, a coasting flywheel
     * stops coasting).
     */
    public void setLive(boolean live) {
        this.live = live;
        for (PlantLoad l : loads) {
            l.setEnergised(live);
        }
    }

    /** Total demand of the energised loads on this bus, in megawatts. */
    public double demandMw() {
        return loads.stream().filter(PlantLoad::isEnergised)
            .mapToDouble(PlantLoad::demandMw).sum();
    }

    /** Total installed load regardless of state, in megawatts. */
    public double connectedMw() {
        return loads.stream().mapToDouble(PlantLoad::ratedMw).sum();
    }

    /** Installed load in one supply class, in megawatts. */
    public double connectedMw(SupplyClass cls) {
        return loads.stream().filter(l -> l.supplyClass() == cls)
            .mapToDouble(PlantLoad::ratedMw).sum();
    }

    @Override
    public String toString() {
        return String.format("%s %skV %.1f/%.1f MW%s",
            name, trim(voltageKv), demandMw(), connectedMw(), live ? "" : " [DEAD]");
    }

    private static String trim(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
    }
}
