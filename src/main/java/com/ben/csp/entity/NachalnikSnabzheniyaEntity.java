package com.ben.csp.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.world.World;
import net.minecraft.entity.LivingEntity;

/**
 * NachalnikSnabzheniyaEntity Entity.
 */
abstract class NachalnikSnabzheniyaEntity extends LivingEntity {

    public static final EntityType<NachalnikSnabzheniyaEntity> BARAGE = null;


    public NachalnikSnabzheniyaEntity(World world) {
        super(BARAGE, world);
    }

    public NachalnikSnabzheniyaEntity(EntityType<? extends NachalnikSnabzheniyaEntity> entityType, World world) {
        super(entityType, world);
    }

}