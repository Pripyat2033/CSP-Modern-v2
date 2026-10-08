package com.ben.csp.block;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * ICMS Panel Block Types.
 */
public class IcmsPanelBlock {

    private static String register(String name) {
        Registry.register(Registries.BLOCK, new Identifier("csp_modern", name), null);
        return "csp_modern:" + name;
    }

    public static final String CONTROL_PANEL = register("control_panel");
    public static final String DISCRETE_ELEMENT = register("discrete_element");
}
