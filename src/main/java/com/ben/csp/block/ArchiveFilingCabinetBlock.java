package com.ben.csp.block;

import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;

public class ArchiveFilingCabinetBlock extends Block {
    public ArchiveFilingCabinetBlock() {
        super(FabricBlockSettings.copyOf(net.minecraft.block.Blocks.IRON_BLOCK));
    }
}
