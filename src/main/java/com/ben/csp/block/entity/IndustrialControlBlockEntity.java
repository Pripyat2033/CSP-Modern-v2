package com.ben.csp.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import com.ben.csp.block.ModBlocks;

public class IndustrialControlBlockEntity extends BlockEntity {
    public static final String ID = "industrial_control_block_entity";
    
    public static final net.minecraft.block.entity.BlockEntityType<IndustrialControlBlockEntity> INDUSTRIAL_CONTROL_BLOCK_ENTITY_TYPE = 
        FabricBlockEntityTypeBuilder.create(
            new FabricBlockEntityTypeBuilder.Factory<IndustrialControlBlockEntity>() {
                @Override
                public IndustrialControlBlockEntity create(BlockPos pos, BlockState state) {
                    return new IndustrialControlBlockEntity(pos, state);
                }
            }, ModBlocks.INDUSTRIAL_CONTROL_BLOCK).build();

    public IndustrialControlBlockEntity(BlockPos pos, BlockState state) {
        super(INDUSTRIAL_CONTROL_BLOCK_ENTITY_TYPE, pos, state);
    }
}
