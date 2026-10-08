package com.ben.csp.entity.ai;

import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * An interface for entities that can be "garrisoned" or posted to a specific location.
 * This is used by AI goals like GarrisonGoal and items like the Director's Planshet.
 */
public interface Garrisonable {

    /**
     * Sets the entity's garrison position. This is their "post" or home base.
     * @param pos The position to garrison at. Can be null to clear the post.
     */
    void setGarrisonPos(@Nullable BlockPos pos);

    /**
     * Gets the entity's current garrison position.
     * @return The garrison position, or null if not set.
     */
    @Nullable
    BlockPos getGarrisonPos();

    void setSitting(boolean sitting);

    boolean isSitting();
}