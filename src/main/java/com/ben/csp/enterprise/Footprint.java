package com.ben.csp.enterprise;

/**
 * Where a facility actually stands, as an axis-aligned rectangle in plan
 * coordinates (X and Z). A facility has no position until it is sited, so this is
 * absent until the survey and construction put it somewhere.
 */
public record Footprint(double minX, double minZ, double maxX, double maxZ) {

    public Footprint {
        if (minX > maxX || minZ > maxZ) {
            throw new IllegalArgumentException("min corner must not exceed max corner");
        }
    }

    public static Footprint of(double x1, double z1, double x2, double z2) {
        return new Footprint(Math.min(x1, x2), Math.min(z1, z2),
                             Math.max(x1, x2), Math.max(z1, z2));
    }

    public double width() { return maxX - minX; }
    public double height() { return maxZ - minZ; }
    public double area() { return width() * height(); }
    public double centerX() { return (minX + maxX) / 2.0; }
    public double centerZ() { return (minZ + maxZ) / 2.0; }

    public boolean contains(double x, double z) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }
}
