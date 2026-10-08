package com.ben.csp.rbmk;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

public class FuelChannelBlockEntity extends BlockEntity {
    public static final int AXIAL_NODES = 24;
    
    private boolean ruptured = false;

    public FuelChannelBlockEntity(BlockPos pos, BlockState state) {
        super(null, pos, state);
    }

    public boolean isRuptured() { return ruptured; }
}
