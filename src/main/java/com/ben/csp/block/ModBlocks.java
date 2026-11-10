package com.ben.csp.block;

import net.minecraft.block.*;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import com.ben.csp.item.GenplanBlockItem;
import com.ben.csp.item.PlanshetBlockItem;
import com.ben.csp.skala.SkalaCoreBlock;
import com.ben.csp.skala.SkalaTapeDriveBlock;
import com.ben.csp.skala.SkalaRequestDeviceBlock;

public class ModBlocks {
    public static final String MOD_ID = "csp-modern";

    public static final Block VERTUSHKA_BLOCK = VertushkaBlock.INSTANCE;
    public static final Block ARCHIVE_FILING_CABINET_BLOCK = new ArchiveFilingCabinetBlock();
    public static final Block GEODETIC_MARKER_BLOCK = new GeodeticMarkerBlock();
    public static final Block GENPLAN_BLOCK = new GenplanBlock(net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings.copyOf(net.minecraft.block.Blocks.IRON_BLOCK).nonOpaque());
    public static final Block PLANSHET_BLOCK = new PlanshetBlock();
    public static final Block PLANSHET_PART_BLOCK = new PlanshetPartBlock();
    public static final Block INDUSTRIAL_CONTROL_BLOCK = IndustrialControlBlock.INSTANCE;
    public static final Block CHISELED_BLOCK = new ChiseledBlock();
    public static final Block MTK_DISPLAY_BLOCK = MtkDisplayBlock.INSTANCE;
    public static final Block TELETYPE_PRINTER_BLOCK = new TeletypePrinterBlock();
    public static final Block FUEL_CHANNEL_BLOCK = FuelChannelBlock.INSTANCE;
    public static final Block GRAPHITE_BLOCK = GraphiteBlock.INSTANCE;
    public static final Block CONTROL_ROD_BLOCK = ControlRodBlock.INSTANCE;
    public static final Block STEAM_SEPARATOR_BLOCK = PlantEquipmentBlock.steamSeparator();
    public static final Block TURBINE_BLOCK = PlantEquipmentBlock.turbine();
    public static final Block CONDENSER_BLOCK = PlantEquipmentBlock.condenser();
    public static final Block MCP_BLOCK = PlantEquipmentBlock.pump();
    public static final Block FEEDWATER_BLOCK = PlantEquipmentBlock.feedwater();
    public static final Block ECCS_ACCUMULATOR_BLOCK = PlantEquipmentBlock.accumulator();
    public static final Block PLANT_PIPE_BLOCK = PlantEquipmentBlock.pipe();
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
        registerWithItem("genplan", GENPLAN_BLOCK, GenplanBlockItem::new);
        registerWithItem("planshet", PLANSHET_BLOCK, PlanshetBlockItem::new);
        register("planshet_part", PLANSHET_PART_BLOCK);
        register("industrial_control", INDUSTRIAL_CONTROL_BLOCK);
        register("chiseled", CHISELED_BLOCK);
        register("mtk_display", MTK_DISPLAY_BLOCK);
        register("teletype_printer", TELETYPE_PRINTER_BLOCK);
        register("fuel_channel", FUEL_CHANNEL_BLOCK);
        register("graphite", GRAPHITE_BLOCK);
        register("control_rod", CONTROL_ROD_BLOCK);
        register("steam_separator", STEAM_SEPARATOR_BLOCK);
        register("turbine", TURBINE_BLOCK);
        register("condenser", CONDENSER_BLOCK);
        register("mcp", MCP_BLOCK);
        register("feedwater", FEEDWATER_BLOCK);
        register("eccs_accumulator", ECCS_ACCUMULATOR_BLOCK);
        register("plant_pipe", PLANT_PIPE_BLOCK);
        register("stockpile", STOCKPILE_BLOCK);
        register("icms_message_input", ICMS_MESSAGE_INPUT_BLOCK);
        register("icms_message_output", ICMS_MESSAGE_OUTPUT_BLOCK);
        register("control_panel", ICMS_CONTROL_PANEL_BLOCK);
        register("skala_core", SKALA_CORE_BLOCK);
        register("skala_tape_drive", SKALA_TAPE_DRIVE_BLOCK);
        register("skala_request_device", SKALA_REQUEST_DEVICE_BLOCK);
    }

    private static void register(String name, Block block) {
        registerWithItem(name, block, BlockItem::new);
    }

    /** Register a block with a specific BlockItem, for a block that needs one. */
    private static void registerWithItem(String name, Block block,
                                         java.util.function.BiFunction<Block, Item.Settings, ? extends Item> itemFactory) {
        Identifier id = Identifier.of(MOD_ID, name);
        Registry.register(Registries.BLOCK, id, block);
        Registry.register(Registries.ITEM, id, itemFactory.apply(block, new Item.Settings()));
    }
}
