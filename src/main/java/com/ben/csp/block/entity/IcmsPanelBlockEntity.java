package com.ben.csp.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.BlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import com.ben.csp.block.ModBlocks;

/**
 * ICMS Panel Block Entity - Industrial Control System panel handler.
 * Manages control panels and discrete element inputs for the Soviet ICMS network.
 */
public class IcmsPanelBlockEntity extends BlockEntity {
    public static final String ID = "icms_panel_block_entity";
    
    public static final BlockEntityType<IcmsPanelBlockEntity> ICMSPANEL_BLOCK_ENTITY_TYPE = 
        FabricBlockEntityTypeBuilder.create(
            new FabricBlockEntityTypeBuilder.Factory<IcmsPanelBlockEntity>() {
                @Override
                public IcmsPanelBlockEntity create(BlockPos pos, BlockState state) {
                    return new IcmsPanelBlockEntity(pos, state);
                }
            }, ModBlocks.ICMS_CONTROL_PANEL_BLOCK).build();

    public IcmsPanelBlockEntity(BlockPos pos, BlockState state) {
        super(ICMSPANEL_BLOCK_ENTITY_TYPE, pos, state);
    }
    
    /**
     * Register this block entity type with the Minecraft registry.
     * Demonstrates proper usage of Registry and Registries imports:
     */
    public static void registerBlockEntity(Identifier id) {
        if (id == null || !id.getNamespace().equals("csp_modern")) {
            id = Identifier.of("csp_modern", "icms_panel_block_entity");
        }
        Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            id,
            ICMSPANEL_BLOCK_ENTITY_TYPE
        );
    }
}
