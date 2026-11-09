package com.ben.csp.item;

import com.ben.csp.block.ModBlocks;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * The mod's own creative tab, matching the itemGroup.csp-modern translation key.
 */
public class ModItemGroups {

    public static final ItemGroup CSP_GROUP = Registry.register(
        Registries.ITEM_GROUP,
        Identifier.of("csp-modern", "main"),
        FabricItemGroup.builder()
            .icon(() -> new ItemStack(ModItems.DIRECTORS_PLANSHET))
            .displayName(Text.translatable("itemGroup.csp-modern"))
            .entries((context, entries) -> {
                entries.add(ModBlocks.ARCHIVE_FILING_CABINET_BLOCK);
                entries.add(ModBlocks.CHISELED_BLOCK);
                entries.add(ModBlocks.ICMS_CONTROL_PANEL_BLOCK);
                entries.add(ModBlocks.FUEL_CHANNEL_BLOCK);
                entries.add(ModBlocks.GEODETIC_MARKER_BLOCK);
                entries.add(ModBlocks.GRAPHITE_BLOCK);
                entries.add(ModBlocks.ICMS_MESSAGE_INPUT_BLOCK);
                entries.add(ModBlocks.ICMS_MESSAGE_OUTPUT_BLOCK);
                entries.add(ModBlocks.INDUSTRIAL_CONTROL_BLOCK);
                entries.add(ModBlocks.MTK_DISPLAY_BLOCK);
                entries.add(ModBlocks.PLANSHET_BLOCK);
                entries.add(ModBlocks.PLANSHET_PART_BLOCK);
                entries.add(ModBlocks.SKALA_CORE_BLOCK);
                entries.add(ModBlocks.SKALA_REQUEST_DEVICE_BLOCK);
                entries.add(ModBlocks.SKALA_TAPE_DRIVE_BLOCK);
                entries.add(ModBlocks.STOCKPILE_BLOCK);
                entries.add(ModBlocks.TELETYPE_PRINTER_BLOCK);
                entries.add(ModBlocks.VERTUSHKA_BLOCK);
                entries.add(ModItems.TELEFONIST_TOOL);
                entries.add(ModItems.SKALA_LINKER_TOOL);
                entries.add(ModItems.CHISEL);
                entries.add(ModItems.MAGNETIC_TAPE_ITEM);
                entries.add(ModItems.DIRECTORS_PLANSHET);
            })
            .build());

    public static void register() {
        // Class-load triggers static registration.
    }
}
