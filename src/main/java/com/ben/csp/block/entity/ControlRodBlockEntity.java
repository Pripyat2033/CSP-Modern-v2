package com.ben.csp.block.entity;

import net.minecraft.block.entity.BlockEntity;

public class ControlRodBlockEntity extends BlockEntity {
    public static final String ID = "".replace("BlockEntity", "_entity");
    
    // Constructor with null parameters - Fabric handles registration internally
    public ControlRodBlockEntity() {
        super(null, null, null);
    }
}
