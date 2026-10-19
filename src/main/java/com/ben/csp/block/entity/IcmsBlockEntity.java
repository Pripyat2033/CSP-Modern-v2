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
 * ICMS Block Entity - Industrial Control System block handler.
 * Manages factory control signals and network communications for the Soviet ICMS.
 */
public class IcmsBlockEntity extends BlockEntity {
    public static final String ID = "icms_block_entity";
    
    /**
     * Block entity type for ICMS message input blocks.
     * Uses FabricBlockEntityTypeBuilder with a factory that handles block creation.
     */
    public static final BlockEntityType<IcmsBlockEntity> ICMS_BLOCK_ENTITY_TYPE = 
        FabricBlockEntityTypeBuilder.create(
            new FabricBlockEntityTypeBuilder.Factory<IcmsBlockEntity>() {
                @Override
                public IcmsBlockEntity create(BlockPos pos, BlockState state) {
                    return new IcmsBlockEntity(pos, state);
                }
            }, ModBlocks.ICMS_MESSAGE_INPUT_BLOCK).build();

    /**
     * Constructor for ICMS block entity.
     * Creates an instance with the given position and block state.
     */
    public IcmsBlockEntity(BlockPos pos, BlockState state) {
        super(ICMS_BLOCK_ENTITY_TYPE, pos, state);
    }
    
    /**
     * Register this block entity type with the Minecraft registry.
     * Demonstrates proper usage of Registry and Registries imports:
     */
    public static void registerBlockEntity(Identifier id) {
        if (id == null || !id.getNamespace().equals("csp_modern")) {
            id = Identifier.of("csp_modern", "icms_block_entity");
        }
        Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            id,
            ICMS_BLOCK_ENTITY_TYPE
        );
    }
}
