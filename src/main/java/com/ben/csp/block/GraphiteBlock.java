package com.ben.csp.block;

import net.minecraft.block.Block;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;

public class GraphiteBlock extends Block {
    public static final String ID = "graphite_block";
    
    public static final GraphiteBlock INSTANCE = new GraphiteBlock();
    
    private GraphiteBlock() {
        super(FabricBlockSettings.copyOf(net.minecraft.block.Blocks.IRON_BLOCK));
    }
}
