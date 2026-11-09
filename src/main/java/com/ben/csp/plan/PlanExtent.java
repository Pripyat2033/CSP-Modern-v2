package com.ben.csp.plan;

/** The piece of the world a drawing covers, in plan coordinates (X and Z). */
public record PlanExtent(double minX, double minZ, double maxX, double maxZ) {

    public PlanExtent {
        if (minX > maxX || minZ > maxZ) {
            throw new IllegalArgumentException("min corner must not exceed max corner");
        }
    }

    public double width() { return maxX - minX; }
    public double height() { return maxZ - minZ; }
    public double centerX() { return (minX + maxX) / 2.0; }
    public double centerZ() { return (minZ + maxZ) / 2.0; }

    public boolean contains(double x, double z) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }
}
