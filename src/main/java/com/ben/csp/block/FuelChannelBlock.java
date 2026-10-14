package com.ben.csp.block;

import net.minecraft.block.Block;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;

public class FuelChannelBlock extends Block {
    public static final String ID = "fuel_channel_block";
    
    public static final FuelChannelBlock INSTANCE = new FuelChannelBlock();
    
    private FuelChannelBlock() {
        super(FabricBlockSettings.copyOf(net.minecraft.block.Blocks.IRON_BLOCK));
    }
}
