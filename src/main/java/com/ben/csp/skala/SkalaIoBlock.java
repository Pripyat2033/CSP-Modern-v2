package com.ben.csp.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;

/**
 * Science Grade: Represents IO cabinetry for the SKALA system.
 * Currently a standard block, but may be upgraded to hold state later.
 */
public class SkalaIoBlock extends Block {
    public SkalaIoBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }
}