package com.ben.csp.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

public class ChiseledBlockEntity extends BlockEntity {
    public ChiseledBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISELED_BLOCK_ENTITY, pos, state);
    }
}
