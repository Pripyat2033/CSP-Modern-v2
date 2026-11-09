package com.ben.csp;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resource.ResourceType;
import com.ben.csp.prombaza.PrombazaCommand;
import com.ben.csp.command.CspCommand;
import com.ben.csp.directorate.DirectorateCommand;
import com.ben.csp.zone.ZoneCommand;
import com.ben.csp.zone.ZoneTicker;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import com.ben.csp.block.ModBlocks;
import com.ben.csp.data.InfrastructureRequirementLoader;
import com.ben.csp.entity.ModEntities;
import com.ben.csp.entity.ModEntityAttributes;
import com.ben.csp.entity.dialogue.DialogueListener;
import com.ben.csp.block.entity.ModBlockEntities;
import com.ben.csp.item.ModItemGroups;
import com.ben.csp.item.ModItems;
import com.ben.csp.item.ModItemsSpawnEggs;
import com.ben.csp.opb.OPB82RuleLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CSP-Modern Mod Entry Point
 * 
 * Historical Context: Chernobyl Scientific Project (ЧСЗО - Чернобыльская Научная Зона)
 * This mod simulates the Soviet-era construction and operations at ChNPP/ChAES.
 */
public class CSPMod implements ModInitializer {

    public static final String MOD_ID = "csp-modern";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // Register all blocks
        ModBlocks.register();
        
        // Register all block entities  
        ModBlockEntities.registerAll();
        
        // Register all items
        ModItems.registerModItems();
        
        // Register all entity types
        ModEntities.init();

        // Default attributes (health/speed) for every entity
        ModEntityAttributes.register();
        
        // Register spawn egg items (must be after entities are registered)
        ModItemsSpawnEggs.registerModSpawnEggs();

        // Creative tab (must be after blocks and items exist)
        ModItemGroups.register();

        // Data-driven rule sets
        ResourceManagerHelper.get(ResourceType.SERVER_DATA)
            .registerReloadListener(new OPB82RuleLoader());
        ResourceManagerHelper.get(ResourceType.SERVER_DATA)
            .registerReloadListener(new InfrastructureRequirementLoader());

        // Commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            PrombazaCommand.register(dispatcher);
            ZoneCommand.register(dispatcher);
            DirectorateCommand.register(dispatcher);
            CspCommand.register(dispatcher);
        });

        // Speak to the workers in plain words: the nearest one answers and acts.
        DialogueListener.register();

        // The zone runs on the server clock: the enterprises build themselves live.
        ServerTickEvents.END_SERVER_TICK.register(ZoneTicker::tick);

        LOGGER.info("CSP-Modern 0.4.0 loaded. Commands: /csp help, /prombaza, "
            + "/directorate, /zone.");
    }
}
