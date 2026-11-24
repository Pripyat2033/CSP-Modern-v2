package com.ben.csp.grid;

/**
 * A two-winding transformer: a source on the high side, a bus on the low side,
 * and a rating. It is the machine that decides where a plant's electricity comes
 * from, so the two transformers here are the two ways a unit can be supplied:
 *
 * <ul>
 *   <li><b>ТСН</b>, the unit auxiliary transformer — tapped off the generator
 *       terminals, so it is alive only while the unit is generating. This is the
 *       normal supply at power, and it is why a turbine trip takes the
 *       auxiliaries with it.</li>
 *   <li><b>РТСН</b>, the reserve auxiliary transformer — tapped off the
 *       switchyard, so it is alive whenever the grid is. This is the standby that
 *       the plant transfers to when the unit trips, and whether that transfer
 *       succeeds is the difference between a trip and a station blackout.</li>
 * </ul>
 *
 * <p>Rating is checked: a transformer cannot carry more than it is rated for, and
 * an overloaded transformer trips rather than magically supplying the load.
 */
public final class Transformer {

    public enum Role {
        /** ТСН — unit auxiliary, off the generator. */
        UNIT_AUXILIARY("ТСН"),
        /** РТСН — reserve auxiliary, off the switchyard. */
        RESERVE_AUXILIARY("РТСН"),
        /** Step-down to 0.4 kV services. */
        SERVICE("6/0.4 kV");

        private final String label;

        Role(String label) { this.label = label; }

        public String label() { return label; }
    }

    private final String id;
    private final Role role;
    /** Rated throughput, MVA (taken as MW at unity power factor here). */
    private final double ratingMva;
    private final Breaker hvBreaker;
    private final Breaker lvBreaker;
    /** The bus this transformer feeds, or null if it feeds nothing yet. */
    private ElectricalBus fedBus;
    private boolean overloadTripped;

    public Transformer(String id, Role role, double ratingMva) {
        this.id = id;
        this.role = role;
        this.ratingMva = ratingMva;
        this.hvBreaker = new Breaker(id + ".hv", role.label() + " HV");
        this.lvBreaker = new Breaker(id + ".lv", role.label() + " LV");
    }

    public String id() { return id; }
    public Role role() { return role; }
    public double ratingMva() { return ratingMva; }
    public Breaker hvBreaker() { return hvBreaker; }
    public Breaker lvBreaker() { return lvBreaker; }
    public ElectricalBus fedBus() { return fedBus; }
    public boolean overloadTripped() { return overloadTripped; }

    public void feed(ElectricalBus bus) {
        this.fedBus = bus;
    }

    /**
     * Decide whether the transformer is passing power, and if so how much it can
     * pass. Returns the megawatts it delivers to its bus, which is zero unless
     * both breakers are closed.
     *
     * <p>The rating is a hard limit: exceeding it trips the transformer instead of
     * silently over-supplying, because an overloaded auxiliary transformer is a
     * real failure mode and not a bookkeeping detail.
     *
     * @param supplyMw what the high side can offer, in megawatts
     * @param demandMw what the fed bus is asking for, in megawatts
     */
    public double deliver(double supplyMw, double demandMw) {
        if (!hvBreaker.isClosed() || !lvBreaker.isClosed()) {
            return 0;
        }
        double wanted = Math.min(supplyMw, demandMw);
        if (wanted > ratingMva) {
            overloadTripped = true;
            hvBreaker.tripByProtection();
            lvBreaker.tripByProtection();
            return 0;
        }
        return wanted;
    }

    /** Reset the overload latch (an operator has inspected and reclosed). */
    public void reset() {
        overloadTripped = false;
        hvBreaker.close();
        lvBreaker.close();
    }

    @Override
    public String toString() {
        return String.format("%s (%s, %.0f MVA)%s", id, role.label(), ratingMva,
            overloadTripped ? " [OVERLOAD TRIPPED]" : "");
    }
}
