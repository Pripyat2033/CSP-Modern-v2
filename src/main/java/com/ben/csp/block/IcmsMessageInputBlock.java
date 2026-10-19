package com.ben.csp.block;

import com.ben.csp.util.BlockSettings;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * ICMS Message Input Block - Soviet Industrial Control System input terminal.
 * Connects to ICMS control network for receiving factory command inputs.
 */
public class IcmsMessageInputBlock extends Block {

    public static final String NAME = "icms_message_input";

    public IcmsMessageInputBlock() {
        super(BlockSettings.create());
    }

    public static void register() {
        Registry.register(Registries.BLOCK, Identifier.of("csp_modern", NAME), new IcmsMessageInputBlock());
    }
}
