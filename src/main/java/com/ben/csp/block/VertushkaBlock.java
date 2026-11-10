package com.ben.csp.block;

import com.ben.csp.telephony.DialGeometry;
import com.ben.csp.telephony.KommutatorExchange;
import com.ben.csp.telephony.TelephonyState;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * The vertushka (KTS) desk telephone: a bakelite body with a rotary dial and a
 * handset. Right-click to lift the handset; put your finger in a dial hole to call a
 * number; right-click again to hang up. The коммутатор does the rest — see
 * {@link KommutatorExchange}.
 */
public class VertushkaBlock extends HorizontalFacingBlock {

    public static final String ID = "vertushka_block";

    /** The last dialled digit, 0-9, shown on the dial. */
    public static final IntProperty DIAL = IntProperty.of("dial", 0, 9);

    private static final VoxelShape SHAPE = Block.createCuboidShape(2, 0, 3.4, 14, 8.7, 13);

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
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        return SHAPE;
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.onPlaced(world, pos, state, placer, stack);
        if (world instanceof ServerWorld sw) {
            int number = TelephonyState.get(sw.getServer()).placeLine(pos);
            if (placer instanceof ServerPlayerEntity sp) {
                sp.sendMessage(Text.literal("Vertushka connected on line " + number + ".")
                    .formatted(Formatting.GRAY), false);
            }
        }
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState,
                                boolean moved) {
        if (!state.isOf(newState.getBlock()) && world instanceof ServerWorld sw) {
            TelephonyState.get(sw.getServer()).removeLine(pos);
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
                              Hand hand, BlockHitResult hit) {
        if (world instanceof ServerWorld sw && player instanceof ServerPlayerEntity sp) {
            use(sw, pos, state, sp, hit);
        }
        return ActionResult.success(world.isClient);
    }

    private static void use(ServerWorld world, BlockPos pos, BlockState state,
                            ServerPlayerEntity player, BlockHitResult hit) {
        MinecraftServer server = world.getServer();
        TelephonyState tel = TelephonyState.get(server);
        int number = tel.lineAt(pos);
        if (number < 0) {
            number = tel.placeLine(pos);
        }
        KommutatorExchange.Line line = tel.exchange().line(number);
        if (line == null) {
            return;
        }

        switch (line.state) {
            case ON_HOOK -> {
                tel.exchange().offHook(number);
                player.sendMessage(Text.literal("You lift the handset on line " + number
                    + ". Put your finger in a hole on the dial to call a number.").formatted(Formatting.GRAY), false);
            }
            case OFF_HOOK, DIALING -> dial(world, pos, state, player, hit, tel, number);
            case RINGING -> {
                if (line.outgoing) {
                    hangUp(world, pos, state, player, tel, number, "You hang up; the call is dropped.");
                } else {
                    tel.exchange().answer(number);
                    world.setBlockState(pos, state.with(DIAL, 0));
                    player.sendMessage(Text.literal("You answer line " + line.peer
                        + ". Speak plainly — say what you need; right-click to hang up.")
                        .formatted(Formatting.GREEN), false);
                }
            }
            case TALKING -> hangUp(world, pos, state, player, tel, number,
                "You hang up. The line is clear.");
            case BUSY -> hangUp(world, pos, state, player, tel, number,
                "You hang up and will try again later.");
        }
    }

    private static void hangUp(ServerWorld world, BlockPos pos, BlockState state,
                               ServerPlayerEntity player, TelephonyState tel, int number,
                               String message) {
        tel.exchange().hangUp(number);
        world.setBlockState(pos, state.with(DIAL, 0));
        world.playSound(null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.4f, 0.6f);
        player.sendMessage(Text.literal(message).formatted(Formatting.GRAY), false);
    }

    private static void dial(ServerWorld world, BlockPos pos, BlockState state,
                             ServerPlayerEntity player, BlockHitResult hit,
                             TelephonyState tel, int number) {
        double dx = hit.getPos().x - pos.getX();
        double dy = hit.getPos().y - pos.getY();
        double dz = hit.getPos().z - pos.getZ();
        int digit = DialGeometry.digitFor(faceOf(state.get(FACING)), dx, dy, dz);
        if (digit < 0) {
            player.sendMessage(Text.literal("Put your finger in one of the holes.")
                .formatted(Formatting.GRAY), true);
            return;
        }
        tel.exchange().dial(number, (char) ('0' + digit));
        world.setBlockState(pos, state.with(DIAL, digit));
        world.playSound(null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS,
            0.4f, 0.8f + digit * 0.04f);
        player.sendMessage(Text.literal("Dialled " + digit + ".").formatted(Formatting.GRAY), true);
    }

    private static DialGeometry.Face faceOf(Direction facing) {
        return switch (facing) {
            case SOUTH -> DialGeometry.Face.SOUTH;
            case EAST -> DialGeometry.Face.EAST;
            case WEST -> DialGeometry.Face.WEST;
            default -> DialGeometry.Face.NORTH;
        };
    }
}
