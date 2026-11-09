package com.ben.csp.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.world.World;

/**
 * Nachalnik Snabzheniya - supply chief. Runs the depot and issues materials.
 */
public class NachalnikSnabzheniyaEntity extends CspPersonEntity {

    public NachalnikSnabzheniyaEntity(EntityType<NachalnikSnabzheniyaEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public WorkerRole getRole() {
        return WorkerRole.NACHALNIK_SNABZHENIYA;
    }
}
