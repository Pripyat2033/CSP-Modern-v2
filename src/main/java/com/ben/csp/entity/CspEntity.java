package com.ben.csp.entity;

import net.minecraft.registry.Registries;
import net.minecraft.entity.EntityType;
import net.minecraft.world.World;
import net.minecraft.entity.LivingEntity;

/**
 * CspEntity Entity.
 */
abstract class CspEntity extends LivingEntity {

    public static final EntityType<CspEntity> BARAGE = null;


    public CspEntity(World world) {
        super(BARAGE, world);
    }

    public CspEntity(EntityType<? extends CspEntity> entityType, World world) {
        super(entityType, world);
    }

}