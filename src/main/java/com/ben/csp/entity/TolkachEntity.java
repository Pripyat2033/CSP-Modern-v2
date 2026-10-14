package com.ben.csp.entity;

import net.minecraft.registry.Registries;
import net.minecraft.entity.EntityType;
import net.minecraft.world.World;
import net.minecraft.entity.LivingEntity;

/**
 * TolkachEntity Entity.
 */
abstract class TolkachEntity extends LivingEntity {

    public static final EntityType<TolkachEntity> BARAGE = null;


    public TolkachEntity(World world) {
        super(BARAGE, world);
    }

    public TolkachEntity(EntityType<? extends TolkachEntity> entityType, World world) {
        super(entityType, world);
    }

}