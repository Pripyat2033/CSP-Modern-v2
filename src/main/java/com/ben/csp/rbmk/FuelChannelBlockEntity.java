package com.ben.csp.rbmk;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;

public class FuelChannelBlockEntity extends BlockEntity {
    public static final String ID = "fuel_channel_block_entity";
    
    // Constructor with null parameters - Fabric handles registration internally
    public FuelChannelBlockEntity() {
        super(null, null, null);
    }
}
