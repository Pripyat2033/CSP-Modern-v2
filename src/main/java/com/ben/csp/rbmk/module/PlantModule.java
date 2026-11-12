package com.ben.csp.rbmk.module;

import java.util.List;

/**
 * A swappable, self-contained piece of plant — the pipework today, another system
 * tomorrow — that can be built and <b>removed</b> on its own without disturbing
 * anything else in the energoblok.
 *
 * <p>The contract that makes it modular:
 * <ul>
 *   <li>A module is <b>pure data</b> (a list of {@link PlantCell}) computed from
 *       the energoblok layout. It never touches the world, so it is testable with
 *       no Minecraft on the classpath and it cannot mutate other structures.</li>
 *   <li>It knows <b>what it owns</b> (its cells), so removal is exact: it clears
 *       only its own cells.</li>
 *   <li>It is <b>disjoint by construction</b> from the core, the compartments and
 *       the machines — {@link ModuleRegistry#audit} refuses a module whose cells
 *       overlap any of them. That is the guarantee that swapping a module cannot
 *       break anything else.</li>
 * </ul>
 */
public interface PlantModule {

    /** A stable, unique id (used by the command and the registry). */
    String id();

    /** A human label for the build log. */
    String label();

    /**
     * Every block this module places, in origin-relative blocks — the reactor
     * centre at the reactor-hall floor, the same frame as {@code EnergoblokLayout}.
     * Pure; may be called for counting or testing, and never touches the world.
     */
    List<PlantCell> cells();

    /** How many cells carry a given role. */
    default long count(PlantCell.Role role) {
        long n = 0;
        for (PlantCell c : cells()) {
            if (c.role() == role) {
                n++;
            }
        }
        return n;
    }
}
