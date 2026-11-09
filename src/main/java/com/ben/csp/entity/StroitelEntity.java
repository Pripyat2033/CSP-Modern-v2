package com.ben.csp.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.world.World;

/**
 * Stroitel - construction worker. Erects the buildings bit by bit.
 */
public class StroitelEntity extends CspPersonEntity {

    public StroitelEntity(EntityType<StroitelEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public WorkerRole getRole() {
        return WorkerRole.STROITEL;
    }
}
