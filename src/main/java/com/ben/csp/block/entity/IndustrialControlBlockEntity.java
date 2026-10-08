package com.ben.csp.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

public class IndustrialControlBlockEntity extends BlockEntity {
    public IndustrialControlBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INDUSTRIAL_CONTROL_BLOCK_ENTITY, pos, state);
    }
}
