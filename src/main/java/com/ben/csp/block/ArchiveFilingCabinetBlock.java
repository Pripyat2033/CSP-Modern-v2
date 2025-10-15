package com.ben.csp.block;

import com.ben.csp.block.entity.ArchiveFilingCabinetBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class ArchiveFilingCabinetBlock extends Block implements BlockEntityProvider {

    public ArchiveFilingCabinetBlock(Settings settings) {
        super(settings);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ArchiveFilingCabinetBlockEntity(pos, state);
    }
}