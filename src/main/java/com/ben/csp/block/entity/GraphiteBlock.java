package com.ben.csp.block.entity;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Graphite Block - RBMK Reactor Moderator Block.
 */
public class GraphiteBlock {

    private static String register(String name) {
        Registry.register(Registries.BLOCK, new Identifier("csp_modern", name), null);
        return "csp_modern:" + name;
    }

    public static final String GRAPHITE_BLOCK = register("graphite_block");
    public static final String MODERATOR_GRAPHITE = register("moderator_graphite");
}
