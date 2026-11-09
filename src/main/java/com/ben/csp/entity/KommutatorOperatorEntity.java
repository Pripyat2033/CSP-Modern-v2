package com.ben.csp.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.world.World;

/**
 * Kommutator - switchboard operator. Garrisons the telephone exchange and
 * connects calls across the enterprise.
 */
public class KommutatorOperatorEntity extends CspPersonEntity {

    public KommutatorOperatorEntity(EntityType<KommutatorOperatorEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public WorkerRole getRole() {
        return WorkerRole.KOMMUTATOR;
    }
}
