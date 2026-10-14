package com.ben.csp.skala;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;

public class SkalaRequestDeviceBlockEntity extends BlockEntity {
    public static final String ID = "skala_request_device_block_entity";
    
    // Constructor with null parameters - Fabric handles registration internally
    public SkalaRequestDeviceBlockEntity() {
        super(null, null, null);
    }
}
