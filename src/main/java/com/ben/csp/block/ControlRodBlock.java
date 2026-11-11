package com.ben.csp.block;

import net.minecraft.block.Block;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;

/** One control channel (СУЗ) of the RBMK core, at 1:4. */
public class ControlRodBlock extends Block {
    public static final String ID = "control_rod_block";

    public static final ControlRodBlock INSTANCE = new ControlRodBlock();

    private ControlRodBlock() {
        // non-opaque: the graphite bore is a real hole with the control rod in it
        super(FabricBlockSettings.copyOf(net.minecraft.block.Blocks.IRON_BLOCK).nonOpaque());
    }
}
