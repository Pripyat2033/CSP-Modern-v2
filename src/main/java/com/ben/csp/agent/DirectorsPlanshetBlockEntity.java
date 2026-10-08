package com.ben.csp.agent;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class DirectorsPlanshetBlockEntity extends BlockEntity {
    // Directors' plansheet block entity
    
    private static final int PLANSHEET_SLOT_COUNT = 9;
    
    public DirectorsPlanshetBlockEntity(BlockEntityType<DirectorsPlanshetBlockEntity> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
}
