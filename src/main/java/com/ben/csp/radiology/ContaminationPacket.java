package com.ben.csp.radiology;

import net.minecraft.util.math.Vec3d;

/**
 * ARL: Represents a discrete packet of contamination percolating down through the soil.
 */
public class ContaminationPacket {
    public final String isotopeName;
    public double activityBq;
    public Vec3d position;

    public ContaminationPacket(String isotopeName, double activityBq, Vec3d position) {
        this.isotopeName = isotopeName;
        this.activityBq = activityBq;
        this.position = position;
    }
}