package com.ben.csp.block;

import com.ben.csp.block.entity.GenplanBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * One block of a wall генплан. Build as many as you like, in any rectangle or
 * shape; each block shows the part of the drawing that falls on its cell.
 */
public class GenplanBlock extends BlockWithEntity {

    /** True once this block holds a drawing to show. */
    public static final BooleanProperty ACTIVE = BooleanProperty.of("active");

    public GenplanBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState()
            .with(Properties.HORIZONTAL_FACING, Direction.NORTH)
            .with(ACTIVE, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(Properties.HORIZONTAL_FACING, ACTIVE);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(Properties.HORIZONTAL_FACING,
            ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state,
                         net.minecraft.entity.LivingEntity placer, net.minecraft.item.ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if (!world.isClient) {
            world.scheduleBlockTick(pos, this, 1);
        }
    }

    @Override
    public void scheduledTick(BlockState state, net.minecraft.server.world.ServerWorld world,
                              BlockPos pos, net.minecraft.util.math.random.Random random) {
        super.scheduledTick(state, world, pos, random);
        if (world.getBlockEntity(pos) instanceof GenplanBlockEntity be) {
            be.refresh();
            boolean active = be.hasRaster();
            if (state.get(ACTIVE) != active) {
                world.setBlockState(pos, state.with(ACTIVE, active));
            }
        }
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        // Refresh neighbours when this part of the panel changes, so a panel
        // re-derives its drawing as it is built or broken.
        if (!world.isClient && !state.isOf(newState.getBlock())) {
            for (Direction d : Direction.values()) {
                BlockPos n = pos.offset(d);
                if (world.getBlockEntity(n) instanceof GenplanBlockEntity) {
                    world.scheduleBlockTick(n, world.getBlockState(n).getBlock(), 1);
                }
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new GenplanBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        // a thin plate on the face it looks along
        return VoxelShapes.fullCube();
    }
}
