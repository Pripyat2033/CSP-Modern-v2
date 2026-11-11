package com.ben.csp.block;

import com.ben.csp.rbmk.plant.MachineSpec;
import com.ben.csp.rbmk.plant.MachineValidator;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Blocks;

/**
 * The controller of a plant multiblock. It is a real cell of the machine's shell
 * — the corner of a box machine, the centre of a drum's end cap. Using it
 * validates the machine: the shell must really be present, or the machine is not
 * formed. This is what turns a pile of casing blocks into a machine.
 */
public class MachineControllerBlock extends Block {

    private final MachineSpec spec;

    public MachineControllerBlock(MachineSpec spec) {
        super(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK));
        this.spec = spec;
    }

    public MachineSpec spec() {
        return spec;
    }

    /** The machine's minimum corner, given where this controller stands. */
    public BlockPos minCorner(BlockPos controller) {
        int[] off = spec.controllerOffset();
        return controller.add(-off[0], -off[1], -off[2]);
    }

    @Override
    @SuppressWarnings("deprecation")
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient && world instanceof ServerWorld) {
            BlockPos min = minCorner(pos);
            MachineValidator.Result r = MachineValidator.validate(world, spec, min, pos);
            Formatting colour = r.formed ? Formatting.GREEN : Formatting.RED;
            player.sendMessage(Text.literal(String.format(
                "%s — %s", spec.displayName, r.describe())).formatted(colour), false);
            player.sendMessage(Text.literal(String.format(
                "  %d x %d x %d blocks at %d, %d, %d",
                spec.sizeX, spec.sizeY, spec.sizeZ,
                min.getX(), min.getY(), min.getZ())).formatted(Formatting.GRAY), false);
        }
        return ActionResult.SUCCESS;
    }
}
