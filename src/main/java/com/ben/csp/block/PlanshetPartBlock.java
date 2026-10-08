package com.ben.csp.block;

import com.ben.csp.util.BlockSettings;

import com.ben.csp.util.BlockPosUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.World;

/**
 * A part of the multi-block Planshet structure. This block has no entity and
 * delegates its behavior to the main PlanshetBlock.
 */
public class PlanshetPartBlock extends Block {

    public PlanshetPartBlock() {
        super(BlockSettings.create());
        setDefaultState(this.stateManager.getDefaultState().with(Properties.AGE_25, 0));
    }

    // This is a common workaround to store a BlockPos in a block state, as there's no native BlockPosProperty.
    // We'll use a custom method to encode/decode the position into an integer.
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(Properties.AGE_25); // Using AGE_25 to store the encoded relative position.
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        BlockPos mainPos = BlockPosUtil.decodeToMainPos(state.get(Properties.AGE_25), pos);
        if (mainPos != null && !mainPos.equals(pos)) { // Ensure we don't break ourselves in a loop
            world.breakBlock(mainPos, !player.isCreative());
        }
        super.onBreak(world, pos, state, player);
    }

    public BlockPos getMainBlockPos(World world, BlockPos pos) {
        return BlockPosUtil.decodeToMainPos(world.getBlockState(pos).get(Properties.AGE_25), pos);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        // Keep parts attached to the main Planshet block
        return state;
    }
}
