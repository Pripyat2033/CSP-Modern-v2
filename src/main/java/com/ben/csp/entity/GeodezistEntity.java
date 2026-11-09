package com.ben.csp.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.world.World;

/**
 * Geodezist - surveyor. Ranges over the site to measure and stake out ground.
 */
public class GeodezistEntity extends CspPersonEntity {

    public GeodezistEntity(EntityType<GeodezistEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public WorkerRole getRole() {
        return WorkerRole.GEODEZIST;
    }
}
