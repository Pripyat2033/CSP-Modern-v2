package com.ben.csp.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.world.World;

/**
 * Barge - floating work platform.
 */
public class BargeEntity extends PathAwareEntity {

    public BargeEntity(EntityType<BargeEntity> entityType, World world) {
        super(entityType, world);
    }
}
