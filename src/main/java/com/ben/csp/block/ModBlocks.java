package com.ben.csp.block;

import net.minecraft.block.*;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import com.ben.csp.skala.SkalaCoreBlock;
import com.ben.csp.skala.SkalaTapeDriveBlock;
import com.ben.csp.skala.SkalaRequestDeviceBlock;

public class ModBlocks {
    public static final String MOD_ID = "csp-modern";

    public static final Block VERTUSHKA_BLOCK = VertushkaBlock.INSTANCE;
    public static final Block ARCHIVE_FILING_CABINET_BLOCK = new ArchiveFilingCabinetBlock();
    public static final Block GEODETIC_MARKER_BLOCK = new GeodeticMarkerBlock();
    public static final Block PLANSHET_BLOCK = new PlanshetBlock();
    public static final Block PLANSHET_PART_BLOCK = new PlanshetPartBlock();
    public static final Block INDUSTRIAL_CONTROL_BLOCK = IndustrialControlBlock.INSTANCE;
    public static final Block CHISELED_BLOCK = new ChiseledBlock();
    public static final Block MTK_DISPLAY_BLOCK = MtkDisplayBlock.INSTANCE;
    public static final Block TELETYPE_PRINTER_BLOCK = new TeletypePrinterBlock();
    public static final Block FUEL_CHANNEL_BLOCK = FuelChannelBlock.INSTANCE;
    public static final Block GRAPHITE_BLOCK = GraphiteBlock.INSTANCE;
    public static final Block STOCKPILE_BLOCK = new StockpileBlock();
    public static final Block ICMS_MESSAGE_INPUT_BLOCK = new IcmsMessageInputBlock();
    public static final Block ICMS_MESSAGE_OUTPUT_BLOCK = new IcmsMessageOutputBlock();
    public static final Block ICMS_CONTROL_PANEL_BLOCK = new IcmsControlPanelBlock();
    public static final Block SKALA_CORE_BLOCK = new SkalaCoreBlock(Block.Settings.create());
    public static final Block SKALA_TAPE_DRIVE_BLOCK = new SkalaTapeDriveBlock(Block.Settings.create());
    public static final Block SKALA_REQUEST_DEVICE_BLOCK = new SkalaRequestDeviceBlock(Block.Settings.create());

    public static void register() {
        register("vertushka", VERTUSHKA_BLOCK);
        register("archive_filing_cabinet", ARCHIVE_FILING_CABINET_BLOCK);
        register("geodetic_marker", GEODETIC_MARKER_BLOCK);
        register("planshet", PLANSHET_BLOCK);
        register("planshet_part", PLANSHET_PART_BLOCK);
        register("industrial_control", INDUSTRIAL_CONTROL_BLOCK);
        register("chiseled", CHISELED_BLOCK);
        register("mtk_display", MTK_DISPLAY_BLOCK);
        register("teletype_printer", TELETYPE_PRINTER_BLOCK);
        register("fuel_channel", FUEL_CHANNEL_BLOCK);
        register("graphite", GRAPHITE_BLOCK);
        register("stockpile", STOCKPILE_BLOCK);
        register("icms_message_input", ICMS_MESSAGE_INPUT_BLOCK);
        register("icms_message_output", ICMS_MESSAGE_OUTPUT_BLOCK);
        register("control_panel", ICMS_CONTROL_PANEL_BLOCK);
        register("skala_core", SKALA_CORE_BLOCK);
        register("skala_tape_drive", SKALA_TAPE_DRIVE_BLOCK);
        register("skala_request_device", SKALA_REQUEST_DEVICE_BLOCK);
    }

    private static void register(String name, Block block) {
        Identifier id = Identifier.of(MOD_ID, name);
        Registry.register(Registries.BLOCK, id, block);
        Registry.register(Registries.ITEM, id, new BlockItem(block, new Item.Settings()));
    }
}
