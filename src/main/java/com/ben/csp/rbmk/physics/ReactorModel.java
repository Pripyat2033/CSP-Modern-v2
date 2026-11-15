package com.ben.csp.rbmk.physics;

/**
 * The reactor core as the plant sees it: one object that evolves in time and
 * reports its state. Three implementations exist:
 *
 * <ul>
 *   <li>{@link UnifiedReactorCore} — <b>the core.</b> The time-dependent spatial
 *       flux (the state itself) with the void / temperature / xenon feedback loop
 *       CLOSED. This is what {@code /rbmk plant start} runs;</li>
 *   <li>{@link ReactorCore} — the lumped point-kinetics core: fast, spatially
 *       averaged, still used by {@code /rbmk stress} and the accident-sequence
 *       tests;</li>
 *   <li>{@link SpatialReactorCore} — a 3D static eigenvalue feeding a lumped
 *       amplitude, kept for comparison ({@code /rbmk plant start 3d}).</li>
 * </ul>
 *
 * <p>All expose the same state, so the plant, the commands and the status read
 * are identical whichever core is running.
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
}
