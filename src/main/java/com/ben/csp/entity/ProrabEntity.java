package com.ben.csp.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.world.World;

/**
 * Prorab - site superintendent. Inspects work and directs the site.
 */
public class ProrabEntity extends CspPersonEntity {

    public ProrabEntity(EntityType<ProrabEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public WorkerRole getRole() {
        return WorkerRole.PRORAB;
    }
}
