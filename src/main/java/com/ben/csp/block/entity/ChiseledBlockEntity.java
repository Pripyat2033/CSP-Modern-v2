package com.ben.csp.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
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
}
