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
 * Reel of magnetic tape; the storage medium for SKALA programs and data.
 */
public class MagneticTapeItem extends Item {

    public MagneticTapeItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.csp-modern.magnetic_tape_item").formatted(Formatting.GRAY));
    }
}
