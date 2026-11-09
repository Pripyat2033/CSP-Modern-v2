package com.ben.csp.block;


/**
 * ICMS Block Types.
 */
public class IcmsBlock {

    private static String register(String name) {
        // Registration removed: this previously called Registry.register(..., null),
        // which throws at runtime. These are identity strings only - no block is
        // registered here. Actual blocks are registered in ModBlocks.
        return "csp-modern:" + name;
    }

    public static final String MESSAGE_INPUT = register("icms_message_input");
    public static final String MESSAGE_OUTPUT = register("icms_message_output");
    public static final String TELETYPE_PRINTER = register("teletype_printer");
}
