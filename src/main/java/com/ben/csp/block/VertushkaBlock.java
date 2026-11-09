package com.ben.csp.block;

import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * The vertushka (KTS) desk telephone: a bakelite body with a rotary dial and a
 * handset. The dial has ten states so it can be turned in play, and the block turns
 * to face the player who places it.
 */
public class VertushkaBlock extends HorizontalFacingBlock {

    public static final String ID = "vertushka_block";

    /** The rotary dial position, 0-9. Right-clicking turns it one step. */
    public static final IntProperty DIAL = IntProperty.of("dial", 0, 9);

    private static final VoxelShape SHAPE = Block.createCuboidShape(3, 0, 4, 13, 5.5, 12);

    public static final VertushkaBlock INSTANCE = new VertushkaBlock();

    private VertushkaBlock() {
        super(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque());
        setDefaultState(getDefaultState().with(FACING, Direction.NORTH).with(DIAL, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, DIAL);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        return SHAPE;
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
                              Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            // Turn the dial one hole. The full exchange is added with the telephone.
            int next = (state.get(DIAL) + 1) % 10;
            world.setBlockState(pos, state.with(DIAL, next));
            world.playSound(null, pos, net.minecraft.sound.SoundEvents.BLOCK_LEVER_CLICK,
                net.minecraft.sound.SoundCategory.BLOCKS, 0.4f, 0.9f + next * 0.03f);
            if (player instanceof net.minecraft.server.network.ServerPlayerEntity sp) {
                sp.sendMessage(net.minecraft.text.Text.literal("Dial: " + next
                    + "  (the exchange is not connected yet)"), true);
            }
        }
        return ActionResult.success(world.isClient);
    }
}
