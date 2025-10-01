package com.ben.csp.util;

import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Utility methods for working with custom entities in the CSP mod.
 */
public class CspEntityUtil {

    /**
     * Finds an entity by its UUID within a given world and casts it to the specified type.
     */
    @Nullable
    public static <T extends Entity> T findEntityByUuid(ServerWorld world, @Nullable UUID uuid, Class<T> entityClass) {
        if (uuid == null) return null;
        Entity entity = world.getEntity(uuid);
        if (entityClass.isInstance(entity)) {
            return entityClass.cast(entity);
        }
        return null;
    }
}