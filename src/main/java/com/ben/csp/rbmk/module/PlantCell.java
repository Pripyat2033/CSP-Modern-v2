package com.ben.csp.rbmk.module;

/**
 * One block a module places, in <b>origin-relative</b> coordinates, with a
 * material <em>role</em> (not a Minecraft type).
 *
 * <p>The role indirection is what keeps a module free of any Minecraft class, so
 * the whole topology can be planned, counted and tested with no game on the
 * classpath. The world builder maps each {@link Role} to the registered block at
 * placement time. A module that needs a block the game does not have yet is a
 * compile-time gap, not a silent stub.
 */
public record PlantCell(int x, int y, int z, Role role) {

    /** What a cell is, independent of the game. */
    public enum Role {
        PIPE,
        VALVE,
        METER
    }
}
