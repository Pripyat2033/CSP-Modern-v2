package com.ben.csp.block;

import com.ben.csp.util.BlockSettings;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;
import com.ben.csp.block.entity.VertushkaBlockEntity;

public class VertushkaBlock extends BlockWithEntity {
    public VertushkaBlock() {
        super(BlockSettings.create());
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new VertushkaBlockEntity(pos, state);
    }
}
