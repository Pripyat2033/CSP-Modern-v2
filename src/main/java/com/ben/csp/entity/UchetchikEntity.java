package com.ben.csp.entity;

import net.minecraft.registry.Registries;
import net.minecraft.entity.EntityType;
import net.minecraft.world.World;
import net.minecraft.entity.LivingEntity;

/**
 * UchetchikEntity Entity.
 */
abstract class UchetchikEntity extends LivingEntity {

    public static final EntityType<UchetchikEntity> BARAGE = null;


    public UchetchikEntity(World world) {
        super(BARAGE, world);
    }

    public UchetchikEntity(EntityType<? extends UchetchikEntity> entityType, World world) {
        super(entityType, world);
    }

}