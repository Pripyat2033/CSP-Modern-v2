package com.ben.csp.radiology;

import net.minecraft.util.math.Vec3d;

/**
 * Represents a source of radiation in the world.
 * @param position The precise location of the source.
 * @param type The type of radiation emitted (ALPHA, BETA, GAMMA, NEUTRON).
 * @param activityBq The activity of the source in Becquerels (decays per second).
 */
public record ContaminationSource(Vec3d position, RadiationType type, double activityBq) {
}