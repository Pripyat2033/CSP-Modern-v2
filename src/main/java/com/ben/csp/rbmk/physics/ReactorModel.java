package com.ben.csp.rbmk.physics;

/**
 * The reactor core as the plant sees it: one object that evolves in time and
 * reports its state.
 *
 * <p><b>There is one core: {@link UnifiedReactorCore}.</b> The time-dependent
 * spatial flux is the state itself, with the void / temperature / xenon feedback
 * loop CLOSED. This is what {@code /rbmk plant start} runs and what the plant,
 * the commands and the status read all use.
 *
 * <p>Two earlier implementations still exist but are retired and unreferenced:
 * {@link ReactorCore} (lumped point kinetics) and {@link SpatialReactorCore}
 * (a static eigenvalue feeding a lumped amplitude). They are kept only under the
 * ZERO DELETION POLICY, for the user to remove. Nothing runs them.
 *
 * <p>All implementations expose the same state, so the plant, the commands and
 * the status read are identical whichever core is running.
 */
public interface ReactorModel {

    /** Advance the core by {@code dt} seconds. */
    void step(double dt);

    /** Current thermal power, as a fraction of nominal (1.0 = 100 %). */
    double power();

    /**
     * Fission power alone, as a fraction of nominal — the chain reaction, excluding
     * the fission-product decay heat that {@link #power()} includes. This is what
     * goes to ~0 on a scram.
     */
    double fissionPower();

    /** Current reactivity, dk/k, measured against the reference critical state. */
    double reactivity();

    /** Control-rod insertion, 0 (fully withdrawn) to 1 (fully inserted). */
    double insertion();

    /** Core-average coolant void fraction, 0..1. */
    double voidFraction();

    /** Core-average fuel temperature, degrees C. */
    double fuelTempC();

    /** Xenon worth relative to the full-power equilibrium (1.0 = equilibrium). */
    double xenonRatio();

    /** Drive the rods to a target insertion (takes manual control from the АР). */
    void setRodTarget(double target);

    /** Hand the rods to the automatic regulator (АР) or take them back. */
    void setAutoRegulator(boolean on);

    /**
     * The reactor period, s: positive while the power rises, infinite when steady,
     * {@link Double#NaN} when it cannot yet be measured. The АЗ-5 rate protection
     * (ГЛАВА 3 item б) trips on this.
     *
     * <p>Default NaN so the retired cores need no change; only the live core
     * overrides it.
     */
    default double reactorPeriod() {
        return Double.NaN;
    }

    /**
     * The power level being held, as a fraction of nominal. The АЗ-5 power-margin
     * protection (ГЛАВА 3 items в, г) compares the live power against it.
     */
    default double powerSetpoint() {
        return 1.0;
    }
}
