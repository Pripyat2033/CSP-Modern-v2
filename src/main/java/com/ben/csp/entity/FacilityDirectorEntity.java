package com.ben.csp.entity;

import net.minecraft.registry.Registries;
import net.minecraft.entity.EntityType;
import net.minecraft.world.World;
import net.minecraft.entity.LivingEntity;

/**
 * FacilityDirectorEntity Entity.
 */
abstract class FacilityDirectorEntity extends LivingEntity {

    public static final EntityType<FacilityDirectorEntity> BARAGE = null;


    public FacilityDirectorEntity(World world) {
        super(BARAGE, world);
    }

    public FacilityDirectorEntity(EntityType<? extends FacilityDirectorEntity> entityType, World world) {
        super(entityType, world);
    }

}