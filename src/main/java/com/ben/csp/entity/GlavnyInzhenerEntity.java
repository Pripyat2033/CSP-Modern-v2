package com.ben.csp.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.world.World;

/**
 * Glavny Inzhener - chief engineer. Garrisons the design office.
 */
public class GlavnyInzhenerEntity extends CspPersonEntity {

    public GlavnyInzhenerEntity(EntityType<GlavnyInzhenerEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public WorkerRole getRole() {
        return WorkerRole.GLAVNY_INZHENER;
    }
}
