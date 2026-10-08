package com.ben.csp.agent;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;

public class PlanerkaSession {
    // Planning session for construction tasks
    
    private static final int DEFAULT_PLANNING_DURATION = 10000;
    
    public static int getDefaultPlanningDuration() {
        return DEFAULT_PLANNING_DURATION;
    }
}
