package com.ben.csp.rbmk.physics;

/**
 * The reactor core as the plant sees it: one object that evolves in time and
 * reports its state. Two implementations exist:
 *
 * <ul>
 *   <li>{@link ReactorCore} — the lumped point-kinetics core: fast, spatially
 *       averaged, good for a quick run and for the accident-sequence tests;</li>
 *   <li>{@link SpatialReactorCore} — the same reaction driven by the full 3D
 *       two-group diffusion solver: the flux is resolved over the real lattice
 *       every tick, which is what "simulating an RBMK-1000 in full 3D" means.</li>
 * </ul>
 *
 * <p>Both expose the same state, so the plant, the commands and the status read
 * are identical whichever core is running.
 */
public interface ReactorModel {

    /** Advance the core by {@code dt} seconds. */
    void step(double dt);

    /** Current thermal power, as a fraction of nominal (1.0 = 100 %). */
    double power();

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
