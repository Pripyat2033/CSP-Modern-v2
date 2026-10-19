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

public class ChiseledBlockEntity extends BlockEntity {
    public static final String ID = "chiseled_block_entity";
    
    // Define the BlockEntityType for THIS class as a static field
    public static final net.minecraft.block.entity.BlockEntityType<ChiseledBlockEntity> CHISELED_BLOCK_ENTITY_TYPE = 
        FabricBlockEntityTypeBuilder.create(
            new FabricBlockEntityTypeBuilder.Factory<ChiseledBlockEntity>() {
                @Override
                public ChiseledBlockEntity create(BlockPos pos, BlockState state) {
                    return new ChiseledBlockEntity(pos, state);
                }
            }, ModBlocks.CHISELED_BLOCK).build();

    // Two-parameter constructor that passes local static field to super()
    public ChiseledBlockEntity(BlockPos pos, BlockState state) {
        super(CHISELED_BLOCK_ENTITY_TYPE, pos, state);
    }
    
    /**
     * Register this block entity type with the Minecraft registry.
     * Demonstrates proper usage of Registry and Registries imports:
     */
    public static void registerBlockEntity(Identifier id) {
        if (id == null || !id.getNamespace().equals("csp_modern")) {
            // Use default identifier for automatic registration
            id = Identifier.of("csp_modern", "chiseled_block_entity");
        }
        Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            id,
            CHISELED_BLOCK_ENTITY_TYPE
        );
    }
}
