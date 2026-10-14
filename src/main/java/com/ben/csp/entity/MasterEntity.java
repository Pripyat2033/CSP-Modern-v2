package com.ben.csp.entity;

import net.minecraft.registry.Registries;
import net.minecraft.entity.EntityType;
import net.minecraft.world.World;
import net.minecraft.entity.LivingEntity;

/**
 * MasterEntity Entity.
 */
abstract class MasterEntity extends LivingEntity {

    public static final EntityType<MasterEntity> BARAGE = null;


    public MasterEntity(World world) {
        super(BARAGE, world);
    }

    public MasterEntity(EntityType<? extends MasterEntity> entityType, World world) {
        super(entityType, world);
    }

}