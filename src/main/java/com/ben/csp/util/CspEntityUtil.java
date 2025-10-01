package com.ben.csp.util;

import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A utility class for common entity-related operations within the CSP mod.
 * This centralizes logic for finding and interacting with entities to avoid
 * code duplication and ensure safety.
 */
public final class CspEntityUtil {

    // Private constructor to prevent instantiation of this utility class.
    private CspEntityUtil() {}

    /**
     * Finds an entity of a specific type by its UUID in a given world.
     * This method safely handles server-side lookups and type checking.
     *
     * @param world The server world to search in.
     * @param uuid The UUID of the entity to find.
     * @param entityClass The class of the entity to cast to.
     * @return The found entity cast to the correct type, or null if not found or not of the correct type.
     */
    @Nullable
    public static <T extends Entity> T findEntityByUuid(ServerWorld world, @Nullable UUID uuid, Class<T> entityClass) {
        if (uuid == null) return null;
        Entity entity = world.getEntity(uuid);
        return entityClass.isInstance(entity) ? entityClass.cast(entity) : null;
    }
}