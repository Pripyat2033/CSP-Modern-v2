package com.ben.csp.radiology;

import net.minecraft.util.math.Vec2f;

/**
 * ARL: Represents a 2D area of contaminated groundwater that spreads over time.
 */
public class GroundwaterPlume {
    public final String isotopeName;
    public double totalActivityBq;
    public Vec2f center;
    public float radius;

    private final double spreadRate; // m per tick
    private final Vec2f flowVector; // direction of groundwater flow

    public GroundwaterPlume(String isotopeName, double initialActivity, Vec2f center, Vec2f flowVector) {
        this.isotopeName = isotopeName;
        this.totalActivityBq = initialActivity;
        this.center = center;
        this.radius = 1.0f; // Starts small
        this.flowVector = flowVector;
        this.spreadRate = 0.001; // Spreads very slowly
    }

    public void tick() {
        // Simulate spread and drift
        this.radius += spreadRate;
        this.center = this.center.add(this.flowVector);
        // Decay is handled by the ContaminationPhysicsManager when calculating dose
    }
}