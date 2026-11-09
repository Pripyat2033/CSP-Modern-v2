package com.ben.csp.prombaza;

import java.util.Objects;

/**
 * An axis-aligned volume of the world, in block coordinates. Used to mark out
 * where a prombaza stands.
 */
public record Area(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

    public Area {
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            throw new IllegalArgumentException(
                "min corner must not exceed max corner");
        }
    }

    /** Build an area from any two corners, in any order. */
    public static Area of(int x1, int y1, int z1, int x2, int y2, int z2) {
        return new Area(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2),
                        Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2));
    }

    public boolean contains(int x, int y, int z) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public boolean intersects(Area other) {
        Objects.requireNonNull(other, "other");
        return minX <= other.maxX && maxX >= other.minX
            && minY <= other.maxY && maxY >= other.minY
            && minZ <= other.maxZ && maxZ >= other.minZ;
    }

    public long volume() {
        return (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }

    public int centerX() { return (minX + maxX) / 2; }
    public int centerY() { return (minY + maxY) / 2; }
    public int centerZ() { return (minZ + maxZ) / 2; }

    public String describe() {
        return "(" + minX + " " + minY + " " + minZ + ") to (" + maxX + " " + maxY + " " + maxZ + ")";
    }
}
