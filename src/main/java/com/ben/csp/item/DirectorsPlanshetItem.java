package com.ben.csp.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * The "Planshet" (Планшет), a map case or clipboard used by high-level project management.
 */
public class DirectorsPlanshetItem extends Item {

    public DirectorsPlanshetItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient()) {
            user.sendMessage(Text.literal("Directorate Status: All systems nominal."), false);
        }
        return TypedActionResult.success(user.getStackInHand(hand));
    }
}
