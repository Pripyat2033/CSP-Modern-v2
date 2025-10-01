package com.ben.csp;

import com.ben.csp.block.ModBlocks;
import com.ben.csp.command.BuildCommand;
import com.ben.csp.command.DialogueCommand;
import com.ben.csp.entity.ModEntities;
import com.ben.csp.item.ModItems;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CSPMod implements ModInitializer {
	public static final String MOD_ID = "csp-modern";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		ModBlocks.registerModBlocks();
		ModItems.registerModItems();
		ModEntities.registerModEntities();

		CommandRegistrationCallback.EVENT.register(BuildCommand::register);
		CommandRegistrationCallback.EVENT.register(DialogueCommand::register);
	}
}