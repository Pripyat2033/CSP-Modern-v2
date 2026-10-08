package com.ben.csp.skala;
import com.ben.csp.util.BlockSettings;

import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * The block representing the V-31M Link Cabinet for the SKALA system.
 * This physical interface allows linking the SKALA computer to the reactor core.
 */
public class SkalaV31MLinkBlock extends BlockWithEntity {
    public SkalaV31MLinkBlock() {
        super(BlockSettings.create());
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new SkalaV31MLinkBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }
}