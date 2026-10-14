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
     * Register all mod items including spawn eggs.
     */
    public static void registerModItems() {
        // Spawn Eggs for all entities - will be added once entity types are registered
        
        Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "telefonist_tool"), 
            new TelefonistToolItem());

        Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "skala_linker_tool"), 
            new SkalaLinkerTool());

        // Chisel - Special tool for chiseled blocks
        Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "chisel"), new ChiselItem());

        // Magnetic Tape Item - storage medium
        Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "magnetic_tape_item"), 
            new MagneticTapeItem());

        // Directors Planshet - tablet for directors
        Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "directors_planshet"), 
            new DirectorsPlanshetItem());
    }
}
