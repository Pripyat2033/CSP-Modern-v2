package com.ben.csp.rbmk.plant;

import net.minecraft.block.Block;

/**
 * A machine of the RBMK plant: its casing, its size in blocks, and its shape.
 * Every figure comes from {@code docs/RBMK-MACHINE-GEOMETRY.md}, which quotes the
 * real dimensions; the sizes here are those dimensions at 1:4.
 *
 * <p>The controller is not a decoration — it is a real cell of the shell. When a
 * player uses it, the machine is validated: the shell must actually be there.
 */
public final class MachineSpec {

    /** How the shell is shaped. A drum is round; the rest are box shells. */
    public enum Shape { BOX_SHELL, CYLINDER_X }

    public final String id;
    public final String displayName;
    public final Block casing;
    public final int sizeX, sizeY, sizeZ;
    public final Shape shape;
    /** For CYLINDER_X: the tube radius; sizeY == sizeZ == 2*radius + 1. */
    public final int radius;

    public MachineSpec(String id, String displayName, Block casing,
                       int sizeX, int sizeY, int sizeZ, Shape shape) {
        this.id = id;
        this.displayName = displayName;
        this.casing = casing;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.shape = shape;
        this.radius = shape == Shape.CYLINDER_X ? (sizeY - 1) / 2 : 0;
    }

    /** Where the controller sits, as an offset from the machine's min corner. */
    public int[] controllerOffset() {
        if (shape == Shape.CYLINDER_X) {
            return new int[] {0, sizeY / 2, sizeZ / 2};   // centre of the end cap
        }
        return new int[] {0, 0, 0};                        // a corner
    }

    /** Is the cell at offset (dx, dy, dz) from the min corner part of the shell? */
    public boolean isShell(int dx, int dy, int dz) {
        if (shape == Shape.CYLINDER_X) {
            boolean cap = dx == 0 || dx == sizeX - 1;
            int cy = sizeY / 2;
            int cz = sizeZ / 2;
            long d2 = (long) (dy - cy) * (dy - cy) + (long) (dz - cz) * (dz - cz);
            if (d2 > (long) radius * radius) {
                return false;
            }
            return cap || d2 > (long) (radius - 1) * (radius - 1);
        }
        return dx == 0 || dx == sizeX - 1
            || dy == 0 || dy == sizeY - 1
            || dz == 0 || dz == sizeZ - 1;
    }

    /** The bounding volume in blocks. */
    public long volume() {
        return (long) sizeX * sizeY * sizeZ;
    }
}
