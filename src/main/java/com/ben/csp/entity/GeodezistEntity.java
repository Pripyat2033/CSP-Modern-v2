package com.ben.csp.entity;

import net.minecraft.registry.Registries;
import net.minecraft.entity.EntityType;
import net.minecraft.world.World;
import net.minecraft.entity.LivingEntity;

/**
 * GeodezistEntity Entity.
 */
abstract class GeodezistEntity extends LivingEntity {

    public static final EntityType<GeodezistEntity> BARAGE = null;


    public GeodezistEntity(World world) {
        super(BARAGE, world);
    }

    public GeodezistEntity(EntityType<? extends GeodezistEntity> entityType, World world) {
        super(entityType, world);
    }

}