package com.ben.csp.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.world.World;

/**
 * Sekretar - secretary. Garrisons the office, handles paperwork and calls.
 */
public class SekretarEntity extends CspPersonEntity {

    public SekretarEntity(EntityType<SekretarEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public WorkerRole getRole() {
        return WorkerRole.SEKRETAR;
    }
}
