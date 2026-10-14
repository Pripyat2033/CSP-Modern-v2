package com.ben.csp.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import com.ben.csp.block.ModBlocks;

public class MtkDisplayBlockEntity extends BlockEntity {
    public static final String ID = "mtk_display_block_entity";
    
    public static final net.minecraft.block.entity.BlockEntityType<MtkDisplayBlockEntity> MTK_DISPLAY_BLOCK_ENTITY_TYPE = 
        FabricBlockEntityTypeBuilder.create(
            new FabricBlockEntityTypeBuilder.Factory<MtkDisplayBlockEntity>() {
                @Override
                public MtkDisplayBlockEntity create(BlockPos pos, BlockState state) {
                    return new MtkDisplayBlockEntity(pos, state);
                }
            }, ModBlocks.MTK_DISPLAY_BLOCK).build();

    public MtkDisplayBlockEntity(BlockPos pos, BlockState state) {
        super(MTK_DISPLAY_BLOCK_ENTITY_TYPE, pos, state);
    }
}
