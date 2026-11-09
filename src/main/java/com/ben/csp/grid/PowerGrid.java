package com.ben.csp.grid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The zone's power grid. The plant feeds it; the buildings on the map draw from
 * it. The grid keeps a real balance: supply against demand, and a frequency that
 * sags when demand outruns supply.
 *
 * <p>Frequency responds to the imbalance. When it falls far enough, automatic
 * frequency load shedding (АЧР) drops the least important load in stages to
 * arrest the fall; if that is not enough, the grid collapses and everything goes
 * dark. This is the literal sense in which the plant powers the map.
 */
public final class PowerGrid {

    /** How far the frequency moves per megawatt of imbalance, in hertz per MW. */
    private static final double HZ_PER_MW = 1.0 / 2000.0;

    private final List<PowerSource> sources = new ArrayList<>();
    private final List<PowerConsumer> consumers = new ArrayList<>();

    private double frequencyHz = Power.NOMINAL_FREQUENCY_HZ;
    private boolean blackout;

    public void addSource(PowerSource source) {
        sources.add(Objects.requireNonNull(source, "source"));
    }

    public void addConsumer(PowerConsumer consumer) {
        consumers.add(Objects.requireNonNull(consumer, "consumer"));
    }

    public List<PowerSource> sources() { return Collections.unmodifiableList(sources); }
    public List<PowerConsumer> consumers() { return Collections.unmodifiableList(consumers); }

    public double frequencyHz() { return frequencyHz; }
    public boolean isBlackout() { return blackout; }

    /** Everything the sources can supply right now, in megawatts. */
    public double supplyMw() {
        return sources.stream().mapToDouble(PowerSource::availableMw).sum();
    }

    /** Everything the energised consumers are drawing, in megawatts. */
    public double demandMw() {
        return consumers.stream().filter(PowerConsumer::isEnergised)
            .mapToDouble(PowerConsumer::demandMw).sum();
    }

    /** Everything that could draw, if all of it were energised, in megawatts. */
    public double totalDemandMw() {
        return consumers.stream().mapToDouble(PowerConsumer::demandMw).sum();
    }

    /** Supply minus demand; negative means the grid is short. */
    public double balanceMw() {
        return supplyMw() - demandMw();
    }

    /** True if the grid is short and load must be shed. */
    public boolean isShort() {
        return balanceMw() < 0;
    }

    /**
     * Advance the grid by one step. Frequency follows the balance; if it falls
     * to the shed trigger, load is dropped in priority order; if it cannot be
     * recovered, the grid collapses.
     */
    public void tick() {
        if (blackout) {
            return;
        }
        frequencyHz += HZ_PER_MW * balanceMw();

        if (frequencyHz <= Power.SHED_TRIGGER_HZ) {
            shedUntilStable();
        }

        if (frequencyHz <= Power.COLLAPSE_HZ) {
            blackout = true;
            for (PowerConsumer c : consumers) {
                c.setEnergised(false);
            }
            frequencyHz = 0.0;
        }
    }

    /** Drop the least important load until the grid is no longer short. */
    private void shedUntilStable() {
        for (PowerConsumer c : shedOrder()) {
            if (balanceMw() >= 0) {
                break;
            }
            c.setEnergised(false);
        }
    }

    /** Sheddable consumers, least important first. */
    private List<PowerConsumer> shedOrder() {
        List<PowerConsumer> order = new ArrayList<>();
        for (LoadPriority p : new LoadPriority[]{
                LoadPriority.SHEDDABLE, LoadPriority.NORMAL, LoadPriority.VITAL}) {
            for (PowerConsumer c : consumers) {
                if (c.priority() == p && c.isEnergised()) {
                    order.add(c);
                }
            }
        }
        return order;
    }

    /** How much load is currently dropped, in megawatts. */
    public double shedMw() {
        return consumers.stream().filter(c -> !c.isEnergised())
            .mapToDouble(PowerConsumer::demandMw).sum();
    }

    /** Restore power after a blackout, once supply is back. */
    public void reenergise() {
        blackout = false;
        frequencyHz = Power.NOMINAL_FREQUENCY_HZ;
        for (PowerConsumer c : consumers) {
            c.setEnergised(true);
        }
    }

    @Override
    public String toString() {
        return String.format("grid %.2f Hz, supply %.0f MW, demand %.0f MW%s",
            frequencyHz, supplyMw(), demandMw(), blackout ? " [BLACKOUT]" : "");
    }
}
