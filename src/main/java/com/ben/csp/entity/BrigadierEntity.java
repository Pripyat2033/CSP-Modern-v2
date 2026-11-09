package com.ben.csp.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.world.World;

/**
 * Brigadier - brigade leader. Supervises a work gang on site.
 */
public class BrigadierEntity extends CspPersonEntity {

    public BrigadierEntity(EntityType<BrigadierEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public WorkerRole getRole() {
        return WorkerRole.BRIGADIER;
    }
}
