package com.ben.csp.rbmk;

import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;

public class ControlRodBlock extends Block {
    public ControlRodBlock() {
        super(FabricBlockSettings.copyOf(net.minecraft.block.Blocks.IRON_BLOCK));
    }
}
