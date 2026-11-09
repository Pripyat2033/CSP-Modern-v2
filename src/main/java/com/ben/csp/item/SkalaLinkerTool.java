package com.ben.csp.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Tool used to link SKALA peripheral blocks (core, tape drive, request device)
 * into a single addressing group.
 */
public class SkalaLinkerTool extends Item {

    public SkalaLinkerTool(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.csp-modern.skala_linker_tool").formatted(Formatting.GRAY));
    }
}
