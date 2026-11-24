package com.ben.csp.grid;

/**
 * What a load will tolerate before it drops out, and what it does while the
 * voltage is off. This is the plant's own classification, separate from
 * {@link LoadPriority}, which is about the town: the town is shed to keep the
 * grid up, while the plant's loads are classified by what happens to the reactor
 * when they stop.
 */
public enum SupplyClass {

    /**
     * No interruption at all is tolerable — protection, instrumentation, the
     * RCPS magnets and the SKALA machine. Fed from the batteries (АБП), which
     * carry through a changeover. Losing this class is losing the ability to see
     * or stop the reactor, so it is the last thing to go dark.
     */
    UNINTERRUPTIBLE("uninterruptible (АБП)"),

    /**
     * Essential: may be interrupted briefly while the diesels start, but must
     * come back. The ECCS pumps, emergency feedwater and emergency lighting.
     * 3 x 5.5 MW of diesel is 16.5 MW, which is why this class is small.
     */
    ESSENTIAL("essential (diesels)"),

    /**
     * Unit auxiliaries: fed from the unit's own generator through the auxiliary
     * transformer (ТСН) while the unit is at power, and from the reserve
     * transformer (РТСН) off the grid when it is not. The main circulating pumps
     * live here. This class is what makes a turbine trip dangerous: the moment
     * the generator goes, its own supply goes with it.
     */
    UNIT_AUXILIARY("unit auxiliary (ТСН)"),

    /** Non-essential plant load: workshops, cranes, ventilation. Shed freely. */
    NON_ESSENTIAL("non-essential");

    private final String label;

    SupplyClass(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
