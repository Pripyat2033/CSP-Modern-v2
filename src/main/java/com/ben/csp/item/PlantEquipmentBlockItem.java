package com.ben.csp.item;

import net.minecraft.block.Block;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The item form of a plant machine. The in-game name stays the Soviet term; the
 * tooltip gives the romanised term and the English meaning, so a player who does
 * not read Russian knows which machine they are looking at — and, with the
 * texture, can tell them apart in the world.
 */
public class PlantEquipmentBlockItem extends BlockItem {

    public PlantEquipmentBlockItem(Block block, Settings settings) {
        super(block, settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip,
                              TooltipContext context) {
        String path = Registries.BLOCK.getId(getBlock()).getPath();
        tooltip.add(Text.translatable("tooltip.csp-modern." + path).formatted(Formatting.GRAY));
    }
}
