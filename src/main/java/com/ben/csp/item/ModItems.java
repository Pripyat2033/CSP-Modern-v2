package com.ben.csp.item;

import com.ben.csp.CSPMod;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Central registry for all custom items defined by the CSP-Modern mod.
 */
public class ModItems {

    public static final Item DIRECTORS_PLANSHET = registerItem("directors_planshet",
            new DirectorsPlanshetItem(new FabricItemSettings().maxCount(1)));

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(CSPMod.MOD_ID, name), item);
    }

    public static void registerModItems() {
        CSPMod.LOGGER.info("Registering ModItems for " + CSPMod.MOD_ID);
    }
}