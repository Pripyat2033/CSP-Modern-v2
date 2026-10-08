package com.ben.csp.item;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.util.Hand;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;
import net.minecraft.util.TypedActionResult;

/**
 * "Science Grade": A tool used by a Telefonist (Telephone Technician) to physically
 * patch a connection between two Vertushka phones. This provides an authentic,
 * in-world method for setting a dial target without unrealistic commands.
 */
public class TelefonistToolItem extends Item {
    private static final String LINK_POS_KEY = "LinkPos";

    public TelefonistToolItem(FabricItemSettings settings) {
        super(settings);
    }

    public TelefonistToolItem() {
        this(new FabricItemSettings());
    }
    
    /**
     * Handle left-click on block - link to first clicked phone
     */
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        if (world.isClient()) return ActionResult.SUCCESS;

        BlockPos clickedPos = context.getBlockPos();
        PlayerEntity player = context.getPlayer();
        ItemStack stack = context.getStack();
        BlockState clickedState = world.getBlockState(clickedPos);

        // Check if it's a Vertushka phone block
        if (clickedState.getBlock() instanceof com.ben.csp.block.VertushkaBlock) {
            return ActionResult.PASS;
        }
        // Otherwise check for other phone blocks

        NbtCompound nbt = stack.getOrCreateNbt();
        if (!nbt.contains(LINK_POS_KEY)) {
            // First use: store the connected block's position as string
            nbt.putString(LINK_POS_KEY, clickedPos.toString());
            stack.setNbt(nbt);
            
            Text text = Text.literal("[Telefonist] Phone connected!");
            player.sendMessage(text);
            return ActionResult.SUCCESS;
        } else {
            // Second use: remove the stored link position  
            nbt.remove(LINK_POS_KEY);
            
            Text text2 = Text.literal("[Telefonist] Link removed.");
            player.sendMessage(text2);
            return ActionResult.SUCCESS;
        }
    }

    /**
     * Handle right-click on block - unlink from current phone
     */
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        
        // Remove link data to disconnect
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.remove(LINK_POS_KEY);
        stack.setNbt(nbt);
        
        return TypedActionResult.success(stack, world.isClient());
    }
}
