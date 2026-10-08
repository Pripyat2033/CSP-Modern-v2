package com.ben.csp.item;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * Skala Linker Tool: Used by Skalists to establish physical connections between 
 * the V-31M data bus network and the Simulation Conductor.
 */
public class SkalaLinkerTool extends Item {
    
    public SkalaLinkerTool(FabricItemSettings settings) {
        super(settings);
    }

    public SkalaLinkerTool() {
        this(new FabricItemSettings());
    }
    
    /**
     * Handle left-click on block - link to Simulation Conductor or V-31M Link Block
     */
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        if (world.isClient()) return ActionResult.SUCCESS;

        // Placeholder implementation - will implement later
        return ActionResult.PASS;
    }
    
    /**
     * Handle right-click to reset linker tool
     */
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        
        // Clear link data to reset tool
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.remove("LinkPos");
        stack.setNbt(nbt);
        
        Text text = Text.literal("[LINKER] Tool reset.");
        user.sendMessage(text);
        
        return TypedActionResult.success(stack, world.isClient());
    }
}
