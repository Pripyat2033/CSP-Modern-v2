package com.ben.csp;

import net.fabricmc.api.ModInitializer;
import com.ben.csp.block.ModBlocks;
import com.ben.csp.entity.ModEntities;
import com.ben.csp.block.entity.ModBlockEntities;
import com.ben.csp.item.ModItems;
import com.ben.csp.item.ModItemsSpawnEggs;

/**
 * CSP-Modern Mod Entry Point
 * 
 * Historical Context: Chernobyl Scientific Project (ЧСЗО - Чернобыльская Научная Зона)
 * This mod simulates the Soviet-era construction and operations at ChNPP/ChAES.
 */
public class CSPMod implements ModInitializer {

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
        
        // Register spawn egg items (must be after entities are registered)
        ModItemsSpawnEggs.registerModSpawnEggs();
    }
}
