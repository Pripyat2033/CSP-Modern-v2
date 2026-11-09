package com.ben.csp.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.world.World;

/**
 * Tolkach - manual labourer. Hauls materials and does the heavy unskilled work.
 */
public class TolkachEntity extends CspPersonEntity {

    public TolkachEntity(EntityType<TolkachEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public WorkerRole getRole() {
        return WorkerRole.TOLKACH;
    }
}
