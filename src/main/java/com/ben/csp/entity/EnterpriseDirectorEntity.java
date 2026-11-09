package com.ben.csp.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.world.World;

/**
 * Enterprise Director. Heads the whole construction enterprise.
 */
public class EnterpriseDirectorEntity extends CspPersonEntity {

    public EnterpriseDirectorEntity(EntityType<EnterpriseDirectorEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public WorkerRole getRole() {
        return WorkerRole.ENTERPRISE_DIRECTOR;
    }
}
