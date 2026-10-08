package com.ben.csp.util;

import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;

/**
 * Helper class for creating block settings compatible with Minecraft 1.20.1
 */
public class BlockSettings {
    /**
     * Create default solid block settings
     */
    public static FabricBlockSettings createSolid() {
        return FabricBlockSettings.create().requiresTool();
    }

    /**
     * Create non-solid block settings (air-like)
     */
    public static FabricBlockSettings createNonSolid() {
        return FabricBlockSettings.create();
    }

    /**
     * Create settings without specific requirements
     */
    public static FabricBlockSettings create() {
        return FabricBlockSettings.create();
    }
}
