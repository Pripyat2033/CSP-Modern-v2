package com.ben.csp.item;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.item.Item;

/**
 * Item registration for CSP-Modern mod.
 */
public class ModItems {
    
    private static final String MOD_ID = "csp-modern";

    /**
     * Register all mod items.
     */
    public static void registerModItems() {
        // Telefonist Tool - patching phones in the world
        Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "telefonist_tool"), new TelefonistToolItem());
        
        // Skala Linker Tool - linking V-31M links to simulation conductor  
        Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "skala_linker_tool"), new SkalaLinkerTool());
    }
}
