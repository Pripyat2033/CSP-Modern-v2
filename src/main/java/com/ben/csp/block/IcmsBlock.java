package com.ben.csp.block;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * ICMS Block Types.
 */
public class IcmsBlock {

    private static String register(String name) {
        Registry.register(Registries.BLOCK, new Identifier("csp_modern", name), null);
        return "csp_modern:" + name;
    }

    public static final String MESSAGE_INPUT = register("icms_message_input");
    public static final String MESSAGE_OUTPUT = register("icms_message_output");
    public static final String TELETYPE_PRINTER = register("teletype_printer");
}
