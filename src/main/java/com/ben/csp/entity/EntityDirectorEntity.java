package com.ben.csp.entity;

import net.minecraft.registry.Registries;
import net.minecraft.entity.EntityType;
import net.minecraft.world.World;
import net.minecraft.entity.LivingEntity;

/**
 * EntityDirectorEntity Entity.
 */
abstract class EntityDirectorEntity extends LivingEntity {

    public static final EntityType<EntityDirectorEntity> BARAGE = null;


    public EntityDirectorEntity(World world) {
        super(BARAGE, world);
    }

    public EntityDirectorEntity(EntityType<? extends EntityDirectorEntity> entityType, World world) {
        super(entityType, world);
    }

}