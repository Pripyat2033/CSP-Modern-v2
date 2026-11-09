package com.ben.csp.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.world.World;

/**
 * Master - section foreman. Supervises construction trades.
 */
public class MasterEntity extends CspPersonEntity {

    public MasterEntity(EntityType<MasterEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public WorkerRole getRole() {
        return WorkerRole.MASTER;
    }
}
