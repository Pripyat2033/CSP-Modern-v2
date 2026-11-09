package com.ben.csp.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.world.World;

/**
 * Uchetchik - record keeper. Garrisons the office and logs work.
 */
public class UchetchikEntity extends CspPersonEntity {

    public UchetchikEntity(EntityType<UchetchikEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public WorkerRole getRole() {
        return WorkerRole.UCHETCHIK;
    }
}
