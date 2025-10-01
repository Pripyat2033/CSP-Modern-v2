package com.ben.csp.entity.ai;

import net.minecraft.util.math.BlockPos;

/**
 * An interface for entities that have a specific garrison point.
 * This allows the GarrisonGoal to be more flexible and decoupled from specific entity types.
 */
public interface Garrisonable {
    /**
     * Gets the position of the garrison point for this entity.
     * This could be a main HQ, a site office, or a temporary bytovka.
     */
    BlockPos getGarrisonPos();
    int getGarrisonRadius();
}