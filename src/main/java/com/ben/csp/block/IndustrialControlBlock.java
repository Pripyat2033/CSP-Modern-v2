package com.ben.csp.block;

import net.minecraft.block.Block;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;

public class IndustrialControlBlock extends Block {
    public static final String ID = "industrial_control_block";
    
    public static final IndustrialControlBlock INSTANCE = new IndustrialControlBlock();
    
    private IndustrialControlBlock() {
        super(FabricBlockSettings.copyOf(net.minecraft.block.Blocks.IRON_BLOCK));
    }
}
