package com.ben.csp.rbmk.plant;

import net.minecraft.block.Block;

import java.util.List;

/**
 * A machine of the RBMK plant: its casing, its size in blocks, and its shape.
 * Every figure comes from {@code docs/RBMK-MACHINE-GEOMETRY.md}, which quotes the
 * real dimensions; the sizes here are those dimensions at 1:4.
 *
 * <p>The controller is not a decoration — it is a real cell of the shell. When a
 * player uses it, the machine is validated: the shell must actually be there.
 *
 * <p>A machine is either a single casing of one shape (a drum, a tank, a pump
 * body) or a <b>composite</b>: several casings under one controller, as the
 * turbogenerator is — five cylinder covers and a generator body on one shaft.
 */
public final class MachineSpec {

    /** How the shell is shaped. A drum is round; the rest are box shells. */
    public enum Shape { BOX_SHELL, CYLINDER_X }

    /**
     * One casing of a composite machine: where it sits (min corner, offset from
     * the machine's min corner), what it is made of, and what shape it is.
     */
    public static final class Part {
        public final String name;
        public final Block casing;
        public final int dx, dy, dz;         // min-corner offset from the machine origin
        public final int sizeX, sizeY, sizeZ;
        public final Shape shape;

        public Part(String name, Block casing, int dx, int dy, int dz,
                    int sizeX, int sizeY, int sizeZ, Shape shape) {
            this.name = name;
            this.casing = casing;
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.sizeZ = sizeZ;
            this.shape = shape;
        }

        /** Is the cell at part-local offset (x, y, z) part of this part's shell? */
        public boolean isShell(int x, int y, int z) {
            return shellOf(shape, sizeX, sizeY, sizeZ, x, y, z);
        }

        public long volume() {
            return (long) sizeX * sizeY * sizeZ;
        }
    }

    public final String id;
    public final String displayName;
    /** The casing of a single-shape machine; null for a composite. */
    public final Block casing;
    public final int sizeX, sizeY, sizeZ;
    public final Shape shape;
    /** For CYLINDER_X: the tube radius; sizeY == sizeZ == 2*radius + 1. */
    public final int radius;
    /** Non-null (and non-empty) only for a composite machine. */
    public final List<Part> parts;

    public MachineSpec(String id, String displayName, Block casing,
                       int sizeX, int sizeY, int sizeZ, Shape shape) {
        this(id, displayName, casing, sizeX, sizeY, sizeZ, shape, null);
    }

    private MachineSpec(String id, String displayName, Block casing,
                        int sizeX, int sizeY, int sizeZ, Shape shape, List<Part> parts) {
        this.id = id;
        this.displayName = displayName;
        this.casing = casing;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.shape = shape;
        this.parts = parts;
        this.radius = shape == Shape.CYLINDER_X ? (sizeY - 1) / 2 : 0;
    }

    /** A machine of several casings under one controller (the turbogenerator). */
    public static MachineSpec composite(String id, String displayName,
                                        int sizeX, int sizeY, int sizeZ,
                                        List<Part> parts) {
        return new MachineSpec(id, displayName, null, sizeX, sizeY, sizeZ, null, parts);
    }

    public boolean isComposite() {
        return parts != null && !parts.isEmpty();
    }

    /** Where the controller sits, as an offset from the machine's min corner. */
    public int[] controllerOffset() {
        if (isComposite()) {
            return new int[] {0, 0, 0};
        }
        if (shape == Shape.CYLINDER_X) {
            return new int[] {0, sizeY / 2, sizeZ / 2};   // centre of the end cap
        }
        return new int[] {0, 0, 0};                        // a corner
    }

    /** Is the cell at offset (dx, dy, dz) from the min corner part of the shell? */
    public boolean isShell(int dx, int dy, int dz) {
        if (isComposite()) {
            for (Part p : parts) {
                int x = dx - p.dx;
                int y = dy - p.dy;
                int z = dz - p.dz;
                if (x >= 0 && x < p.sizeX && y >= 0 && y < p.sizeY && z >= 0 && z < p.sizeZ
                    && p.isShell(x, y, z)) {
                    return true;
                }
            }
            return false;
        }
        return shellOf(shape, sizeX, sizeY, sizeZ, dx, dy, dz);
    }

    private static boolean shellOf(Shape shape, int sx, int sy, int sz,
                                   int dx, int dy, int dz) {
        if (shape == Shape.CYLINDER_X) {
            int r = (sy - 1) / 2;
            boolean cap = dx == 0 || dx == sx - 1;
            int cy = sy / 2;
            int cz = sz / 2;
            long d2 = (long) (dy - cy) * (dy - cy) + (long) (dz - cz) * (dz - cz);
            if (d2 > (long) r * r) {
                return false;
            }
            return cap || d2 > (long) (r - 1) * (r - 1);
        }
        return dx == 0 || dx == sx - 1
            || dy == 0 || dy == sy - 1
            || dz == 0 || dz == sz - 1;
    }

    /** The bounding volume in blocks. */
    public long volume() {
        return (long) sizeX * sizeY * sizeZ;
    }
}
