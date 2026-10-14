package com.ben.csp.rbmk;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;

public class SimulationConductorBlockEntity extends BlockEntity {
    public static final String ID = "simulation_conductor_block_entity";
    
    // Constructor with null parameters - Fabric handles registration internally
    public SimulationConductorBlockEntity() {
        super(null, null, null);
    }
}
