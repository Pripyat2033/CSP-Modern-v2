package com.ben.csp.block;

import com.ben.csp.util.BlockSettings;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * ICMS Message Output Block - Soviet Industrial Control System output terminal.
 * Connects to ICMS control network for sending factory status outputs.
 */
public class IcmsMessageOutputBlock extends Block {

    public static final String NAME = "icms_message_output";

    public IcmsMessageOutputBlock() {
        super(BlockSettings.create());
    }

    public static void register() {
        Registry.register(Registries.BLOCK, Identifier.of("csp_modern", NAME), new IcmsMessageOutputBlock());
    }
}
