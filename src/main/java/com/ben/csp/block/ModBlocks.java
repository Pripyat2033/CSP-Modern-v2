package com.ben.csp.block;

import com.ben.csp.CSPMod;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Central registry for all custom blocks defined by the CSP-Modern mod.
 * Follows the standard Fabric pattern for block and block item registration.
 */
public class ModBlocks {

    // --- Block Definitions ---
    public static Block VERTUSHKA_BLOCK;
    public static Block ARCHIVE_FILING_CABINET_BLOCK;

    // --- Block Entity Type Definitions ---
    public static BlockEntityType<VertushkaBlockEntity> VERTUSHKA_BLOCK_ENTITY;


    /**
     * Registers a block and its corresponding BlockItem with the game.
     * @param name The registry name for the block (e.g., "vertushka_block").
     * @param block The block instance to register.
     * @return The registered block instance.
     */
    private static Block registerBlock(String name, Block block) {
        registerBlockItem(name, block);
        return Registry.register(Registries.BLOCK, new Identifier(CSPMod.MOD_ID, name), block);
    }

    /**
     * Registers a BlockItem for a given block.
     * @param name The registry name.
     * @param block The block to create an item for.
     * @return The registered item instance.
     */
    private static Item registerBlockItem(String name, Block block) {
        return Registry.register(Registries.ITEM, new Identifier(CSPMod.MOD_ID, name),
                new BlockItem(block, new FabricItemSettings()));
    }

    public static void registerModBlocks() {
        CSPMod.LOGGER.info("Registering ModBlocks for " + CSPMod.MOD_ID);

        VERTUSHKA_BLOCK = registerBlock("vertushka_block",
                new VertushkaBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque()));
        ARCHIVE_FILING_CABINET_BLOCK = registerBlock("archive_filing_cabinet_block",
                new Block(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK)));

        VERTUSHKA_BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE,
                new Identifier(CSPMod.MOD_ID, "vertushka_block_entity"),
                FabricBlockEntityTypeBuilder.create(VertushkaBlockEntity::new, VERTUSHKA_BLOCK).build());
    }
}