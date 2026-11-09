package com.ben.csp.item;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Item registration for CSP-Modern mod.
 */
public class ModItems {

    private static final String MOD_ID = "csp-modern";

    public static final Item TELEFONIST_TOOL =
        new TelefonistToolItem(new Item.Settings().maxCount(1));
    public static final Item SKALA_LINKER_TOOL =
        new SkalaLinkerTool(new Item.Settings().maxCount(1));
    public static final Item CHISEL =
        new ChiselItem(new Item.Settings().maxCount(1));
    public static final Item MAGNETIC_TAPE_ITEM =
        new MagneticTapeItem(new Item.Settings().maxCount(16));
    public static final Item DIRECTORS_PLANSHET =
        new DirectorsPlanshetItem(new Item.Settings().maxCount(1));

    public static void registerModItems() {
        register("telefonist_tool", TELEFONIST_TOOL);
        register("skala_linker_tool", SKALA_LINKER_TOOL);
        register("chisel", CHISEL);
        register("magnetic_tape_item", MAGNETIC_TAPE_ITEM);
        register("directors_planshet", DIRECTORS_PLANSHET);
    }

    private static void register(String name, Item item) {
        Registry.register(Registries.ITEM, Identifier.of(MOD_ID, name), item);
    }
}
