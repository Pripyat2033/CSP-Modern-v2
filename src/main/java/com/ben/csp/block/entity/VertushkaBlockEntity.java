package com.ben.csp.block.entity;

import com.ben.csp.block.ModBlocks;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

public class VertushkaBlockEntity extends BlockEntity {
    public VertushkaBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.VERTUSHKA_BLOCK_ENTITY, pos, state);
    }
}