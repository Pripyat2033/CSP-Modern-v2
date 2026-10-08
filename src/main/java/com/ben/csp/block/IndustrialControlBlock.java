package com.ben.csp.block;

import com.ben.csp.util.BlockSettings;

import com.ben.csp.block.entity.IndustrialControlBlockEntity;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class IndustrialControlBlock extends BlockWithEntity {
    public IndustrialControlBlock() {
        super(BlockSettings.create());
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new IndustrialControlBlockEntity(pos, state);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }

        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof IndustrialControlBlockEntity controlBlockEntity)) {
            return ActionResult.FAIL;
        }

        // Handle block interaction logic here
        // This would typically involve opening a GUI or executing commands via the IndustrialControlBlockEntity
        return ActionResult.SUCCESS;
    }
}
