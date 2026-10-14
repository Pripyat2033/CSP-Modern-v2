package com.ben.csp.entity;

import net.minecraft.registry.Registries;
import net.minecraft.entity.EntityType;
import net.minecraft.world.World;
import net.minecraft.entity.LivingEntity;

/**
 * GlavnyInzhenerEntity Entity.
 */
abstract class GlavnyInzhenerEntity extends LivingEntity {

    public static final EntityType<GlavnyInzhenerEntity> BARAGE = null;


    public GlavnyInzhenerEntity(World world) {
        super(BARAGE, world);
    }

    public GlavnyInzhenerEntity(EntityType<? extends GlavnyInzhenerEntity> entityType, World world) {
        super(entityType, world);
    }

}