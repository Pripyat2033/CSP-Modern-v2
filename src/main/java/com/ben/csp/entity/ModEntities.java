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


    public static void registerModEntities() {
        CSPMod.LOGGER.info("Registering Mod Entities for " + CSPMod.MOD_ID);
        FabricDefaultAttributeRegistry.register(BARGE, BargeEntity.createBargeAttributes());
        FabricDefaultAttributeRegistry.register(GEODEZIST, GeodezistEntity.createGeodezistAttributes());
    }
}