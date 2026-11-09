package com.ben.csp.grid;

/** The real quantities of the power system. */
public final class Power {

    private Power() {}

    /** Nominal grid frequency in the USSR, in hertz. */
    public static final double NOMINAL_FREQUENCY_HZ = 50.0;

    /** The rated electrical output of one RBMK-1000 unit, in megawatts. */
    public static final double RBMK_UNIT_MW = 1000.0;

    /**
     * Frequency at or below which automatic frequency load shedding (АЧР)
     * begins to drop load, in hertz.
     */
    public static final double SHED_TRIGGER_HZ = 49.0;

    /**
     * Frequency the shedding tries to recover to, in hertz. Shedding stops once
     * the frequency is back above this.
     */
    public static final double SHED_RECOVERY_HZ = 49.5;

    /** Frequency below which the grid collapses, in hertz. */
    public static final double COLLAPSE_HZ = 45.0;
}
