package com.ben.csp.block;

import com.ben.csp.util.BlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A special Block that acts as a wrapper for any base block. 
 * Used to chisel the appearance of existing blocks without changing their ID.
 */
public class ChiseledBlock extends Block {
    public ChiseledBlock() {
        super(BlockSettings.create());
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }
}
