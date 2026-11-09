package com.ben.csp.block.entity;


/**
 * BitMaterial - Bit-scale material definitions for 1:1 scale simulation.
 */
public class BitMaterial {

    private static String register(String name) {
        // Registration removed: this previously called Registry.register(..., null),
        // which throws at runtime. These are identity strings only - no block is
        // registered here. Actual blocks are registered in ModBlocks.
        return "csp-modern:" + name;
    }

    public static final String STRUCTURAL_CONCRETE = register("structural_concrete");
    public static final String STRUCTURAL_REINFORCED_CONCRETE = register("structural_reinforced_concrete");
    public static final String STRUCTURAL_STEEL_BEAM = register("structural_steel_beam");
    public static final String GRAPHITE_BLOCK = register("graphite_block");
    public static final String ASBESTOS_INSULATION = register("asbestos_insulation");
    public static final String STEEL_PIPE_SMALL = register("steel_pipe_small");
    public static final String LEAD_LINE_COOLANT_PIPE = register("lead_line_coolant_pipe");
    public static final String COPPER_WIRE_2MM = register("copper_wire_2mm");
    public static final String BUSBAR_BRONZE = register("busbar_bronze");
    public static final String MODERATOR_GRAPHITE_CHANNEL = register("moderator_graphite_channel");
}
