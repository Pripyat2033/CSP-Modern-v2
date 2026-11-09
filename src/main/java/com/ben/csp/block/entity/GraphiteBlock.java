package com.ben.csp.block.entity;


/**
 * Graphite Block - RBMK Reactor Moderator Block.
 */
public class GraphiteBlock {

    private static String register(String name) {
        // Registration removed: this previously called Registry.register(..., null),
        // which throws at runtime. These are identity strings only - no block is
        // registered here. Actual blocks are registered in ModBlocks.
        return "csp-modern:" + name;
    }

    public static final String GRAPHITE_BLOCK = register("graphite_block");
    public static final String MODERATOR_GRAPHITE = register("moderator_graphite");
}
