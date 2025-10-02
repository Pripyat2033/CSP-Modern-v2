package com.ben.csp.entity.ai;

import net.minecraft.util.math.BlockPos;

/**
 * An interface for entities that can be "garrisoned" at a specific location.
 * This is used to make NPCs return to their post when idle.
 */
public interface Garrisonable {
    /**
     * @return The position where this entity should be garrisoned.
     */
    BlockPos getGarrisonPos();
    int getGarrisonRadius();
}