package com.ben.csp.entity;

import net.minecraft.registry.Registries;
import net.minecraft.entity.EntityType;
import net.minecraft.world.World;
import net.minecraft.entity.LivingEntity;

/**
 * SekretarEntity Entity.
 */
abstract class SekretarEntity extends LivingEntity {

    public static final EntityType<SekretarEntity> BARAGE = null;


    public SekretarEntity(World world) {
        super(BARAGE, world);
    }

    public SekretarEntity(EntityType<? extends SekretarEntity> entityType, World world) {
        super(entityType, world);
    }

}