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

public class VertushkaBlockEntity extends BlockEntity {
    public static final String ID = "vertushka_block_entity";
    
    public static final net.minecraft.block.entity.BlockEntityType<VertushkaBlockEntity> VERTUSHKA_BLOCK_ENTITY_TYPE = 
        FabricBlockEntityTypeBuilder.create(
            new FabricBlockEntityTypeBuilder.Factory<VertushkaBlockEntity>() {
                @Override
                public VertushkaBlockEntity create(BlockPos pos, BlockState state) {
                    return new VertushkaBlockEntity(pos, state);
                }
            }, ModBlocks.VERTUSHKA_BLOCK).build();

    public VertushkaBlockEntity(BlockPos pos, BlockState state) {
        super(VERTUSHKA_BLOCK_ENTITY_TYPE, pos, state);
    }
    
    /**
     * Register this block entity type with the Minecraft registry.
     * Demonstrates proper usage of Registry and Registries imports:
     */
    public static void registerBlockEntity(Identifier id) {
        if (id == null || !id.getNamespace().equals("csp_modern")) {
            id = Identifier.of("csp_modern", "vertushka_block_entity");
        }
        Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            id,
            VERTUSHKA_BLOCK_ENTITY_TYPE
        );
    }
}
