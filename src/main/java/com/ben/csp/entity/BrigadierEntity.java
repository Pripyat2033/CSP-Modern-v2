package com.ben.csp.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.world.World;

/**
 * Brigadier - work brigade leader.
 */
public class BrigadierEntity extends CspPersonEntity {

    public BrigadierEntity(EntityType<BrigadierEntity> entityType, World world) {
        super(entityType, world);
    }
}
