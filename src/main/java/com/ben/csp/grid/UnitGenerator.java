package com.ben.csp.grid;

import java.util.Objects;

/**
 * A generating unit feeding the switchyard: one RBMK-1000 unit's two
 * turbogenerators, seen from the grid as a single 1,000 MWe source.
 *
 * <p>The source exists so the plant can be a {@link PowerSource} to the zone
 * grid without the grid needing to know what a reactor is. It reports what the
 * turbines are actually producing right now, so a trip, a rundown or a
 * coast-down shows up on the grid immediately.
 */
public final class UnitGenerator implements PowerSource {

    private final String id;
    private final String name;
    /** Rated electrical output, MW. */
    private final double ratedMw;
    /** Current electrical output, MW. */
    private double outputMw;
    /** True while the generator is synchronised to the switchyard. */
    private boolean synchronised = true;
    /** The breaker between the generator and the switchyard. */
    private final Breaker breaker;

    public UnitGenerator(String id, String name, double ratedMw) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.ratedMw = ratedMw;
        this.breaker = new Breaker(id + ".gcb", name + " generator breaker");
    }

    @Override public String id() { return id; }
    public String name() { return name; }
    public double ratedMw() { return ratedMw; }
    public Breaker breaker() { return breaker; }
    public boolean isSynchronised() { return synchronised && breaker.isClosed(); }

    /** Update the output from the turbines, in megawatts. */
    public void setOutputMw(double mw) {
        this.outputMw = Math.max(0, Math.min(ratedMw, mw));
    }

    public double outputMw() { return outputMw; }

    @Override
    public double availableMw() {
        return isSynchronised() ? outputMw : 0;
    }

    /** The unit trips: the generator breaker opens and the output collapses. */
    public void trip() {
        breaker.loseSupply();
        synchronised = false;
        outputMw = 0;
    }

    /** Re-synchronise after a trip (the operator's job, not automatic). */
    public void synchronise() {
        breaker.close();
        synchronised = true;
    }

    @Override
    public String toString() {
        return String.format("%s %.0f/%.0f MW%s", name, availableMw(), ratedMw,
            isSynchronised() ? "" : " [OFF]");
    }
}
