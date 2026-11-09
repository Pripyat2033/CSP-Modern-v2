package com.ben.csp.entity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

/**
 * Registers the default attribute container for every CSP entity. Without this
 * the game logs "Entity csp-modern:x has no attributes" and the mob cannot move.
 */
public class ModEntityAttributes {

    public static void register() {
        FabricDefaultAttributeRegistry.register(ModEntities.TOLKACH, CspPersonEntity.createPersonAttributes());
        FabricDefaultAttributeRegistry.register(ModEntities.STROITEL, CspPersonEntity.createPersonAttributes());
        FabricDefaultAttributeRegistry.register(ModEntities.PRORAB, CspPersonEntity.createPersonAttributes());
        FabricDefaultAttributeRegistry.register(ModEntities.BRIGADIER, CspPersonEntity.createPersonAttributes());
        FabricDefaultAttributeRegistry.register(ModEntities.MASTER, CspPersonEntity.createPersonAttributes());
        FabricDefaultAttributeRegistry.register(ModEntities.GEODEZIST, CspPersonEntity.createPersonAttributes());
        FabricDefaultAttributeRegistry.register(ModEntities.UCHETCHIK, CspPersonEntity.createPersonAttributes());
        FabricDefaultAttributeRegistry.register(ModEntities.SEKRETAR, CspPersonEntity.createPersonAttributes());
        FabricDefaultAttributeRegistry.register(ModEntities.NACHALNIK_SNABZHENIYA, CspPersonEntity.createPersonAttributes());
        FabricDefaultAttributeRegistry.register(ModEntities.ENTERPRISE_DIRECTOR, CspPersonEntity.createPersonAttributes());
        FabricDefaultAttributeRegistry.register(ModEntities.FACILITY_DIRECTOR, CspPersonEntity.createPersonAttributes());
        FabricDefaultAttributeRegistry.register(ModEntities.GLAVNY_INZHENER, CspPersonEntity.createPersonAttributes());
        FabricDefaultAttributeRegistry.register(ModEntities.KOMMUTATOR, CspPersonEntity.createPersonAttributes());
        FabricDefaultAttributeRegistry.register(ModEntities.BARGE, BargeEntity.createBargeAttributes());
    }
}
