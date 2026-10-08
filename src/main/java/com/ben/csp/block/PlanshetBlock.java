package com.ben.csp.block;

import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;

public class PlanshetBlock extends Block {
    public PlanshetBlock() {
        super(FabricBlockSettings.copyOf(net.minecraft.block.Blocks.IRON_BLOCK));
    }
}
