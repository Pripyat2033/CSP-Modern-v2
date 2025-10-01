package com.ben.csp.radiology;

import net.minecraft.block.Block;

import java.util.HashMap;
import java.util.Map;

public class SurfaceDepositionRegistry {
    private static final SurfaceDepositionRegistry INSTANCE = new SurfaceDepositionRegistry();
    private final Map<Block, DepositionProperties> propertiesMap = new HashMap<>();
    private static final DepositionProperties DEFAULT_PROPERTIES = new DepositionProperties(0.5, 0.5, 0.1); // Default for generic blocks

    private SurfaceDepositionRegistry() {}

    public static SurfaceDepositionRegistry getInstance() {
        return INSTANCE;
    }

    public void loadProperties(Map<Block, DepositionProperties> loadedProperties) {
        propertiesMap.clear();
        propertiesMap.putAll(loadedProperties);
    }

    public DepositionProperties getProperties(Block block) {
        return propertiesMap.getOrDefault(block, DEFAULT_PROPERTIES);
    }
}