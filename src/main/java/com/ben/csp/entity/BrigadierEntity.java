package com.ben.csp.entity;

import net.minecraft.registry.Registries;
import net.minecraft.entity.EntityType;
import net.minecraft.world.World;
import net.minecraft.entity.LivingEntity;

/**
 * BrigadierEntity Entity.
 */
abstract class BrigadierEntity extends LivingEntity {

    public static final EntityType<BrigadierEntity> BARAGE = null;


    public BrigadierEntity(World world) {
        super(BARAGE, world);
    }

    public BrigadierEntity(EntityType<? extends BrigadierEntity> entityType, World world) {
        super(entityType, world);
    }

}