package com.ben.csp.skala;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;

public class SkalaCoreBlockEntity extends BlockEntity {
    public static final String ID = "skala_core_block_entity";
    
    // Constructor with null parameters - Fabric handles registration internally
    public SkalaCoreBlockEntity() {
        super(null, null, null);
    }
}
