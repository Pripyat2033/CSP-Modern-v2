package com.ben.csp.block.entity;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntities {

    public static void registerAll() {
        Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of("csp-modern", "chiseled_block_entity"), ChiseledBlockEntity.CHISELED_BLOCK_ENTITY_TYPE);
        Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of("csp-modern", "graphite_block_entity"), GraphiteBlockEntity.GRAPHITE_BLOCK_ENTITY_TYPE);
        Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of("csp-modern", "icms_block_entity"), IcmsBlockEntity.ICMS_BLOCK_ENTITY_TYPE);
        Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of("csp-modern", "icms_panel_block_entity"), IcmsPanelBlockEntity.ICMSPANEL_BLOCK_ENTITY_TYPE);
        Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of("csp-modern", "industrial_control_block_entity"), IndustrialControlBlockEntity.INDUSTRIAL_CONTROL_BLOCK_ENTITY_TYPE);
        Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of("csp-modern", "mtk_display_block_entity"), MtkDisplayBlockEntity.MTK_DISPLAY_BLOCK_ENTITY_TYPE);
        Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of("csp-modern", "vertushka_block_entity"), VertushkaBlockEntity.VERTUSHKA_BLOCK_ENTITY_TYPE);
    }
}
