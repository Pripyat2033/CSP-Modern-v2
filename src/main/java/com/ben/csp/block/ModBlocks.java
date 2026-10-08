package com.ben.csp.block;

import net.minecraft.block.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlocks {
    public static final Block VERTUSHKA_BLOCK = new VertushkaBlock();
    public static final Block ARCHIVE_FILING_CABINET_BLOCK = new ArchiveFilingCabinetBlock();
    public static final Block GEODETIC_MARKER_BLOCK = new GeodeticMarkerBlock();
    public static final Block PLANSHET_BLOCK = new PlanshetBlock();
    public static final Block PLANSHET_PART_BLOCK = new PlanshetPartBlock();
    public static final Block INDUSTRIAL_CONTROL_BLOCK = new IndustrialControlBlock();
    public static final Block CHISELED_BLOCK = new ChiseledBlock();
    public static final Block MTK_DISPLAY_BLOCK = new MtkDisplayBlock();
    public static final Block TELETYPE_PRINTER_BLOCK = new TeletypePrinterBlock();
    public static final Block FUEL_CHANNEL_BLOCK = new FuelChannelBlock();
    public static final Block GRAPHITE_BLOCK = new GraphiteBlock();
    public static final Block STOCKPILE_BLOCK = new StockpileBlock();

    public static void register() {
        Registry.register(Registries.BLOCK, Identifier.of("csp_modern", "vertushka"), VERTUSHKA_BLOCK);
        Registry.register(Registries.BLOCK, Identifier.of("csp_modern", "archive_filing_cabinet"), ARCHIVE_FILING_CABINET_BLOCK);
        Registry.register(Registries.BLOCK, Identifier.of("csp_modern", "geodetic_marker"), GEODETIC_MARKER_BLOCK);
        Registry.register(Registries.BLOCK, Identifier.of("csp_modern", "planshet"), PLANSHET_BLOCK);
        Registry.register(Registries.BLOCK, Identifier.of("csp_modern", "planshet_part"), PLANSHET_PART_BLOCK);
        Registry.register(Registries.BLOCK, Identifier.of("csp_modern", "industrial_control"), INDUSTRIAL_CONTROL_BLOCK);
        Registry.register(Registries.BLOCK, Identifier.of("csp_modern", "chiseled"), CHISELED_BLOCK);
        Registry.register(Registries.BLOCK, Identifier.of("csp_modern", "mtk_display"), MTK_DISPLAY_BLOCK);
        Registry.register(Registries.BLOCK, Identifier.of("csp_modern", "teletype_printer"), TELETYPE_PRINTER_BLOCK);
        Registry.register(Registries.BLOCK, Identifier.of("csp_modern", "fuel_channel"), FUEL_CHANNEL_BLOCK);
        Registry.register(Registries.BLOCK, Identifier.of("csp_modern", "graphite"), GRAPHITE_BLOCK);
        Registry.register(Registries.BLOCK, Identifier.of("csp_modern", "stockpile"), STOCKPILE_BLOCK);
    }

    public static void registerModBlocks() {
        register();
    }

    public static void registerItemGroups() {
        // Item groups registration can be added here when needed
    }
}
