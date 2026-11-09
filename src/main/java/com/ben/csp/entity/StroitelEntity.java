package com.ben.csp.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.world.World;

/**
 * Stroitel - construction worker.
 */
public class StroitelEntity extends CspPersonEntity {

    public StroitelEntity(EntityType<StroitelEntity> entityType, World world) {
        super(entityType, world);
    }
}
