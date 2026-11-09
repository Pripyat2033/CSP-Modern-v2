package com.ben.csp.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.world.World;

/**
 * Shared base for all CSP human personnel. Concrete subclasses are the entities
 * registered in ModEntities and rendered by the client renderers.
 */
public abstract class CspPersonEntity extends PathAwareEntity {

    protected CspPersonEntity(EntityType<? extends CspPersonEntity> entityType, World world) {
        super(entityType, world);
    }

    /** Whether the entity is currently seated. Drives the sitting pose in the models. */
    public boolean isSitting() {
        return false;
    }
}
