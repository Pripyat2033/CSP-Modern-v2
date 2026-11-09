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
 * Field telephone handset used by the Telefonist to establish and test
 * cabled connections between communications blocks.
 */
public class TelefonistToolItem extends Item {

    public TelefonistToolItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.csp-modern.telefonist_tool").formatted(Formatting.GRAY));
    }
}
