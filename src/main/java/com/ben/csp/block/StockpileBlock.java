package com.ben.csp.block;

import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;

public class StockpileBlock extends Block {
    public StockpileBlock() {
        super(FabricBlockSettings.copyOf(net.minecraft.block.Blocks.OBSIDIAN));
    }
}
