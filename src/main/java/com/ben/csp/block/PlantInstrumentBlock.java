package com.ben.csp.block;

import com.ben.csp.block.entity.PlantInstrumentBlockEntity;
import com.ben.csp.block.entity.PlantMeterBlockEntity;
import com.ben.csp.block.entity.PlantValveBlockEntity;
import com.ben.csp.rbmk.RbmkLattice;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A plant field instrument — a flow-control valve or a ball flow meter on the
 * МЦК pipework. Each instrument is bound to the fuel channel it stands on: when
 * it is placed it finds the nearest lattice channel and hands that (x,z) to its
 * block entity, which then reads the live value from the running plant.
 */
public class PlantInstrumentBlock extends BlockWithEntity {

    /** The instrument reads a channel; a pipe or other block does not. */
    public enum Kind { VALVE, METER }

    private final Kind kind;

    public PlantInstrumentBlock(Kind kind, Settings settings) {
        super(settings);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
                         ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if (world.getBlockEntity(pos) instanceof PlantInstrumentBlockEntity be) {
            int[] ch = nearestChannel(pos.getX(), pos.getZ());
            be.setChannel(ch[0], ch[1]);
        }
    }

    /** The lattice channel nearest a world (x,z), in lattice coordinates. */
    private static int[] nearestChannel(int wx, int wz) {
        List<RbmkLattice.Channel> chs = RbmkLattice.channels();
        int bx = 0, bz = 0;
        long best = Long.MAX_VALUE;
        for (RbmkLattice.Channel c : chs) {
            long d = (long) (c.x() - wx) * (c.x() - wx) + (long) (c.z() - wz) * (c.z() - wz);
            if (d < best) {
                best = d;
                bx = c.x();
                bz = c.z();
            }
        }
        return new int[] {bx, bz};
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return kind == Kind.VALVE ? new PlantValveBlockEntity(pos, state)
                                  : new PlantMeterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state,
                                                                 BlockEntityType<T> type) {
        if (world.isClient) {
            return null; // the client only reads the synced value, it does not tick
        }
        if (kind == Kind.VALVE) {
            return (w, p, s, be) -> PlantValveBlockEntity.tick(w, p, s, (PlantValveBlockEntity) be);
        }
        return (w, p, s, be) -> PlantMeterBlockEntity.tick(w, p, s, (PlantMeterBlockEntity) be);
    }

    @Override
    public net.minecraft.block.BlockRenderType getRenderType(BlockState state) {
        return net.minecraft.block.BlockRenderType.MODEL;
    }
}
