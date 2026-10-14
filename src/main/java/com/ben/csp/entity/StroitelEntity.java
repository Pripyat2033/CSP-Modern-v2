package com.ben.csp.entity;

import net.minecraft.registry.Registries;
import net.minecraft.entity.EntityType;
import net.minecraft.world.World;
import net.minecraft.entity.LivingEntity;

/**
 * StroitelEntity Entity.
 */
abstract class StroitelEntity extends LivingEntity {

    public static final EntityType<StroitelEntity> BARAGE = null;


    public StroitelEntity(World world) {
        super(BARAGE, world);
    }

    public StroitelEntity(EntityType<? extends StroitelEntity> entityType, World world) {
        super(entityType, world);
    }

}