package com.ben.csp.skala;
import com.ben.csp.util.BlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;

import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class SkalaRequestDeviceBlock extends BlockWithEntity {
    public SkalaRequestDeviceBlock() {
        super(BlockSettings.create());
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new SkalaRequestDeviceBlockEntity(pos, state);
    }

    @Override public BlockRenderType getRenderType(BlockState state) { return BlockRenderType.MODEL; }
}