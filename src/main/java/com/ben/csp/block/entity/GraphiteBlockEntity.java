package com.ben.csp.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

public class GraphiteBlockEntity extends BlockEntity {
    public GraphiteBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GRAPHITE_BLOCK_ENTITY, pos, state);
    }
}
