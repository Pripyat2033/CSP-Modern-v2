package com.ben.csp.agent;

import net.minecraft.world.entity.Entity;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerLevel;

public class DirectorateManager {
    // Manages all agents and construction operations
    
    private static final int MAX_AGENT_COUNT = 100;
    
    public static int getMaxAgentCount() {
        return MAX_AGENT_COUNT;
    }
}
