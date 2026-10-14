package com.ben.csp.entity;

import net.minecraft.registry.Registries;
import net.minecraft.entity.EntityType;
import net.minecraft.world.World;
import net.minecraft.entity.LivingEntity;

/**
 * EnterpriseDirectorEntity Entity.
 */
abstract class EnterpriseDirectorEntity extends LivingEntity {

    public static final EntityType<EnterpriseDirectorEntity> BARAGE = null;


    public EnterpriseDirectorEntity(World world) {
        super(BARAGE, world);
    }

    public EnterpriseDirectorEntity(EntityType<? extends EnterpriseDirectorEntity> entityType, World world) {
        super(entityType, world);
    }

}