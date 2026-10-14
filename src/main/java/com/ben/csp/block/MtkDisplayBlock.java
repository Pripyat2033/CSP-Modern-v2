package com.ben.csp.block;

import net.minecraft.block.Block;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;

public class MtkDisplayBlock extends Block {
    public static final String ID = "mtk_display_block";
    
    public static final MtkDisplayBlock INSTANCE = new MtkDisplayBlock();
    
    private MtkDisplayBlock() {
        super(FabricBlockSettings.copyOf(net.minecraft.block.Blocks.IRON_BLOCK));
    }
}
