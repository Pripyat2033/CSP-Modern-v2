package com.ben.csp.radiology;

import net.minecraft.util.math.Vec3d;

/**
 * ARL: The ultimate lightweight data object representing a single quantum of radiation.
 * This is NOT an entity. It is a ray to be traced by our custom engine.
 */
public class RadiationParticle {
    public final RadiationType type;
    public final Vec3d origin;
    public final Vec3d direction;
    public double energyMeV;

    public RadiationParticle(RadiationType type, Vec3d origin, Vec3d direction, double energyMeV) {
        this.type = type;
        this.origin = origin;
        this.direction = direction;
        this.energyMeV = energyMeV;
    }
}