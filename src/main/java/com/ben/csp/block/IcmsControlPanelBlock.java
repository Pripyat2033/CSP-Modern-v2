package com.ben.csp.block;

import com.ben.csp.util.BlockSettings;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * ICMS Control Panel Block - Soviet Industrial Control System main control panel.
 * Used for factory command and monitoring in the ICMS network.
 */
public class IcmsControlPanelBlock extends Block {

    public static final String NAME = "control_panel";

    public IcmsControlPanelBlock() {
        super(BlockSettings.create());
    }

    public static void register() {
        Registry.register(Registries.BLOCK, Identifier.of("csp_modern", NAME), new IcmsControlPanelBlock());
    }
}
