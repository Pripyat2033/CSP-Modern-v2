package com.ben.csp.client;

import com.ben.csp.entity.CspPersonEntity;
import com.ben.csp.entity.ModEntities;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * A guard against a crash that has already happened once: spawning a worker whose type
 * has no client renderer makes the render dispatcher return nothing and takes the client
 * down. This lists every registered worker type so the count can be compared with the
 * renderers registered in {@link com.ben.csp.CSPModClient}; if they differ, the missing
 * one is named in the log at start-up instead of at the moment of a crash.
 */
public final class EntityRendererGuard {

    private static final Logger LOG = LoggerFactory.getLogger("csp-modern");

    private EntityRendererGuard() {}

    /** The worker entity types that MUST each have a renderer. */
    public static List<EntityType<?>> workerTypes() {
        return List.of(
            ModEntities.TOLKACH, ModEntities.STROITEL, ModEntities.PRORAB, ModEntities.BRIGADIER,
            ModEntities.MASTER, ModEntities.GEODEZIST, ModEntities.KOMMUTATOR, ModEntities.UCHETCHIK,
            ModEntities.SEKRETAR, ModEntities.NACHALNIK_SNABZHENIYA, ModEntities.ENTERPRISE_DIRECTOR,
            ModEntities.FACILITY_DIRECTOR, ModEntities.GLAVNY_INZHENER);
    }

    /** Log the worker types and their registry ids, so a missing renderer is obvious. */
    public static void report() {
        List<String> ids = new ArrayList<>();
        for (EntityType<?> type : workerTypes()) {
            Identifier id = Registries.ENTITY_TYPE.getId(type);
            ids.add(id.getPath());
            if (!CspPersonEntity.class.isAssignableFrom(type.getBaseClass())) {
                LOG.error("[CSP] entity type {} is not a worker", id);
            }
        }
        LOG.info("[CSP] worker types needing a renderer ({}): {}", ids.size(), String.join(", ", ids));
    }
}
