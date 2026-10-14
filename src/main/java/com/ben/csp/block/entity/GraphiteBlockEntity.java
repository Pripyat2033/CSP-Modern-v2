package com.ben.csp.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import com.ben.csp.block.ModBlocks;

public class GraphiteBlockEntity extends BlockEntity {
    public static final String ID = "graphite_block_entity";
    
    public static final net.minecraft.block.entity.BlockEntityType<GraphiteBlockEntity> GRAPHITE_BLOCK_ENTITY_TYPE = 
        FabricBlockEntityTypeBuilder.create(
            new FabricBlockEntityTypeBuilder.Factory<GraphiteBlockEntity>() {
                @Override
                public GraphiteBlockEntity create(BlockPos pos, BlockState state) {
                    return new GraphiteBlockEntity(pos, state);
                }
            }, ModBlocks.GRAPHITE_BLOCK).build();

    public GraphiteBlockEntity(BlockPos pos, BlockState state) {
        super(GRAPHITE_BLOCK_ENTITY_TYPE, pos, state);
    }
}
