package com.ben.csp.item;

import net.minecraft.block.Block;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The item form of the генплан block. The name stays the Soviet term; the tooltip
 * gives the romanised term and what it means, so a player who does not read
 * Russian still knows what they are holding.
 */
public class GenplanBlockItem extends BlockItem {

    public GenplanBlockItem(Block block, Settings settings) {
        super(block, settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.csp-modern.genplan").formatted(Formatting.GRAY));
    }
}
