package com.ben.csp.plan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Works out which display cells make up a panel. A panel is a set of wall blocks
 * that touch each other face to face and all face the same way; this finds the
 * connected group, its facing, and gives each block a cell index across and down
 * the panel, so the rasteriser knows where to draw each part of the drawing.
 */
public final class PlanPanelGeometry {

    /** A block of the panel and its cell position (column across, row down). */
    public record Cell(long pos, int col, int row) {}

    private PlanPanelGeometry() {}

    /**
     * Lay out a panel. Facing is the horizontal direction the panel looks along:
     * 0 = +X, 1 = +Z, 2 = -X, 3 = -Z. Across runs to the right as seen looking
     * along the facing; down runs from the top (the block with the greatest Y).
     */
    public static Panel layout(List<Long> blocks, int facing) {
        Objects.requireNonNull(blocks, "blocks");
        if (blocks.isEmpty()) {
            throw new IllegalArgumentException("a panel must have at least one block");
        }
        // right-hand axis for the facing, in XZ
        int rightX = 0, rightZ = 0;
        switch (Math.floorMod(facing, 4)) {
            case 0 -> { rightX = 0; rightZ = 1; }   // looking +X, right is +Z
            case 1 -> { rightX = -1; rightZ = 0; }  // looking +Z, right is -X
            case 2 -> { rightX = 0; rightZ = -1; }  // looking -X, right is -Z
            default -> { rightX = 1; rightZ = 0; }  // looking -Z, right is +X
        }
        final int rx = rightX, rz = rightZ;

        int minAcross = Integer.MAX_VALUE, maxAcross = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (long p : blocks) {
            int across = unpackX(p) * rx + unpackZ(p) * rz;
            int y = unpackY(p);
            minAcross = Math.min(minAcross, across);
            maxAcross = Math.max(maxAcross, across);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
        int cols = maxAcross - minAcross + 1;
        int rows = maxY - minY + 1;

        List<Cell> cells = new ArrayList<>();
        for (long p : blocks) {
            int across = unpackX(p) * rx + unpackZ(p) * rz;
            int y = unpackY(p);
            int col = across - minAcross;
            int row = maxY - y;   // top row first
            cells.add(new Cell(p, col, row));
        }
        return new Panel(facing, cols, rows, cells);
    }

    private static int unpackX(long p) { return (int) (p >> 38); }
    private static int unpackY(long p) { return (int) ((p >> 12) & 0x3FFFFFF); }
    private static int unpackZ(long p) { return (int) (p & 0xFFF); }

    /** Pack a BlockPos as a long: x<<38 | y<<12 | z (z in 12 bits, signed). */
    public static long pack(int x, int y, int z) {
        return ((long) x << 38) | ((long) (y & 0x3FFFFFF) << 12) | (z & 0xFFF);
    }

    public static int unpackXFrom(long p) { return unpackX(p); }
    public static int unpackYFrom(long p) { return unpackY(p); }
    public static int unpackZFrom(long p) { return unpackZ(p); }

    /** A laid-out panel: its facing, its size in cells, and its cells. */
    public static final class Panel {
        private final int facing;
        private final int cols;
        private final int rows;
        private final List<Cell> cells;

        Panel(int facing, int cols, int rows, List<Cell> cells) {
            this.facing = facing;
            this.cols = cols;
            this.rows = rows;
            this.cells = Collections.unmodifiableList(cells);
        }

        public int facing() { return facing; }
        public int cols() { return cols; }
        public int rows() { return rows; }
        public int blockCount() { return cells.size(); }
        public List<Cell> cells() { return cells; }

        /** Is this panel a complete rectangle (every cell filled)? */
        public boolean isComplete() {
            return cells.size() == cols * rows;
        }
    }
}
