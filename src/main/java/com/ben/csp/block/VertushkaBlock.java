package com.ben.csp.block;

import net.minecraft.block.Block;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;

public class VertushkaBlock extends Block {
    public static final String ID = "vertushka_block";
    
    public static final VertushkaBlock INSTANCE = new VertushkaBlock();
    
    private VertushkaBlock() {
        super(FabricBlockSettings.copyOf(net.minecraft.block.Blocks.IRON_BLOCK));
    }
}
