package com.ben.csp.radiology;

import net.minecraft.util.math.Vec3d;

/**
 * ARL: A lightweight data object representing a discrete "hot particle".
 * This is NOT an entity and is managed entirely by the ContaminationPhysicsManager.
 */
public class HotParticle {
    public final Isotope isotope;
    public final double activityBq;
    public final double physicalSize;
    public Vec3d position;
    public Vec3d velocity;

    public HotParticle(Isotope isotope, double activityBq, double physicalSize, Vec3d position) {
        this.isotope = isotope;
        this.activityBq = activityBq;
        this.physicalSize = physicalSize;
        this.position = position;
        this.velocity = Vec3d.ZERO;
    }
}