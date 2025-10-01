package com.ben.csp.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

/**
 * Represents the "Vertushka," a key piece of Soviet-era communication equipment.
 * This block requires a BlockEntity to handle its custom rendering and potential future logic.
 */
public class VertushkaBlock extends Block implements BlockEntityProvider {

    public VertushkaBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        // We return INVISIBLE because the rendering will be handled entirely by the BlockEntityRenderer.
        return BlockRenderType.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        // This will be implemented when we restore the VertushkaBlockEntity file.
        return new VertushkaBlockEntity(pos, state);
    }
}