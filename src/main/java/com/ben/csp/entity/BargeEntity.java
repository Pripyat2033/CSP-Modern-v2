package com.ben.csp.entity;

import net.minecraft.registry.Registries;
import net.minecraft.entity.EntityType;
import net.minecraft.world.World;
import net.minecraft.entity.LivingEntity;

/**
 * Barge Entity.
 */
abstract class BargeEntity extends LivingEntity {

    public static final EntityType<BargeEntity> BARAGE = null;


    public BargeEntity(World world) {
        super(BARAGE, world);
    }

    public BargeEntity(EntityType<? extends BargeEntity> entityType, World world) {
        super(entityType, world);
    }

}