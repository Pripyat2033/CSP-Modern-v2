package com.ben.csp.block;


/**
 * ICMS Panel Block Types.
 */
public class IcmsPanelBlock {

    private static String register(String name) {
        // Registration removed: this previously called Registry.register(..., null),
        // which throws at runtime. These are identity strings only - no block is
        // registered here. Actual blocks are registered in ModBlocks.
        return "csp-modern:" + name;
    }

    public static final String CONTROL_PANEL = register("control_panel");
    public static final String DISCRETE_ELEMENT = register("discrete_element");
}
