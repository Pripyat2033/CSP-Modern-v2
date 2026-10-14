package com.ben.csp.entity;

import net.minecraft.registry.Registries;
import net.minecraft.entity.EntityType;
import net.minecraft.world.World;
import net.minecraft.entity.LivingEntity;

/**
 * ProrabEntity Entity.
 */
abstract class ProrabEntity extends LivingEntity {

    public static final EntityType<ProrabEntity> BARAGE = null;


    public ProrabEntity(World world) {
        super(BARAGE, world);
    }

    public ProrabEntity(EntityType<? extends ProrabEntity> entityType, World world) {
        super(entityType, world);
    }

}