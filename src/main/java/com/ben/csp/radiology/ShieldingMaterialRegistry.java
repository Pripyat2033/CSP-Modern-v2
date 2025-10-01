package com.ben.csp.radiology;

import net.minecraft.block.Block;

import java.util.HashMap;
import java.util.Map;

public class ShieldingMaterialRegistry {
    private static final ShieldingMaterialRegistry INSTANCE = new ShieldingMaterialRegistry();
    private final Map<Block, ShieldingProperties> propertiesMap = new HashMap<>();

    private ShieldingMaterialRegistry() {
        // Constructor is now empty. All data is loaded from JSON.
    }

    public static ShieldingMaterialRegistry getInstance() {
        return INSTANCE;
    }

    public void loadProperties(Map<Block, ShieldingProperties> loadedProperties) {
        propertiesMap.clear();
        propertiesMap.putAll(loadedProperties);
    }

    public ShieldingProperties getProperties(Block block) {
        // Return properties or a default (no shielding) if not registered.
        return propertiesMap.getOrDefault(block, new ShieldingProperties(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
    }
}