package com.ben.csp.entity;

import com.ben.csp.CSPMod;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEntities {
    public static final EntityType<BargeEntity> BARGE = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(CSPMod.MOD_ID, "barge"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, BargeEntity::new)
                    .dimensions(EntityDimensions.fixed(2.0f, 1.0f)).build());
    
    public static final EntityType<GeodezistEntity> GEODEZIST = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(CSPMod.MOD_ID, "geodezist"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, GeodezistEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.8f)).build());

    public static final EntityType<ProrabEntity> PRORAB = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(CSPMod.MOD_ID, "prorab"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, ProrabEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.8f)).build());
    
    public static final EntityType<GlavnyInzhenerEntity> GLAVNY_INZHENER = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(CSPMod.MOD_ID, "glavny_inzhener"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, GlavnyInzhenerEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.8f)).build());

    public static final EntityType<MasterEntity> MASTER = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(CSPMod.MOD_ID, "master"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, MasterEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.8f)).build());

    public static final EntityType<StroitelEntity> STROITEL = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(CSPMod.MOD_ID, "stroitel"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, StroitelEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.8f)).build());

    public static final EntityType<BrigadierEntity> BRIGADIER = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(CSPMod.MOD_ID, "brigadier"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, BrigadierEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.8f)).build());




    public static void registerModEntities() {
        CSPMod.LOGGER.info("Registering Mod Entities for " + CSPMod.MOD_ID);
        FabricDefaultAttributeRegistry.register(BARGE, BargeEntity.createBargeAttributes());
        FabricDefaultAttributeRegistry.register(GEODEZIST, GeodezistEntity.createGeodezistAttributes());
        FabricDefaultAttributeRegistry.register(PRORAB, ProrabEntity.createProrabAttributes());
        FabricDefaultAttributeRegistry.register(GLAVNY_INZHENER, GlavnyInzhenerEntity.createGlavnyInzhenerAttributes());
        FabricDefaultAttributeRegistry.register(MASTER, MasterEntity.createMasterAttributes());
        FabricDefaultAttributeRegistry.register(STROITEL, StroitelEntity.createStroitelAttributes());
        FabricDefaultAttributeRegistry.register(BRIGADIER, BrigadierEntity.createBrigadierAttributes());
    }
}