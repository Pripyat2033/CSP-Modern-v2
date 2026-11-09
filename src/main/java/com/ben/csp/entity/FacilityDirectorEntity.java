package com.ben.csp.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.world.World;

/**
 * Facility Director. Runs one facility of the enterprise.
 */
public class FacilityDirectorEntity extends CspPersonEntity {

    public FacilityDirectorEntity(EntityType<FacilityDirectorEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public WorkerRole getRole() {
        return WorkerRole.FACILITY_DIRECTOR;
    }
}
