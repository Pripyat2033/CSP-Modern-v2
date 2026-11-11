package com.ben.csp.rbmk;

import com.ben.csp.block.ModBlocks;
import com.ben.csp.rbmk.plant.PlantLayout;
import net.minecraft.block.Block;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes the RBMK core and the rest of the plant as real blocks at 1:4.
 *
 * <p>The geometry is expressed as {@link ConstructionJob} plans — a handful of
 * region descriptors — so the build can be either <em>metered</em> (drained a few
 * thousand blocks per tick by {@link ConstructionScheduler}, the normal path) or
 * <em>immediate</em> (filled in one go, used only to measure the burst cost).
 * The core is placed solid (one block per lattice cell); the plant machines are
 * placed as hollow shells, so their form is visible without writing millions of
 * blocks — a turbine hall at 1:4 is 156 blocks long.
 */
public final class RbmkWorldBuilder {

    private RbmkWorldBuilder() {}

    /** Block counts from a build. */
    public static final class Counts {
        public long graphite, fuel, rods, separators, turbines, condensers, pumps,
            feedwater, accumulators, pipes, total;
    }

    private static Counts counts(ConstructionJob job) {
        Counts c = new Counts();
        c.graphite = job.placed(ConstructionJob.Part.GRAPHITE);
        c.fuel = job.placed(ConstructionJob.Part.FUEL);
        c.rods = job.placed(ConstructionJob.Part.RODS);
        c.separators = job.placed(ConstructionJob.Part.SEPARATORS);
        c.turbines = job.placed(ConstructionJob.Part.TURBINES);
        c.condensers = job.placed(ConstructionJob.Part.CONDENSERS);
        c.pumps = job.placed(ConstructionJob.Part.PUMPS);
        c.feedwater = job.placed(ConstructionJob.Part.FEEDWATER);
        c.accumulators = job.placed(ConstructionJob.Part.ACCUMULATORS);
        c.pipes = job.placed(ConstructionJob.Part.PIPES);
        c.total = c.graphite + c.fuel + c.rods + c.separators + c.turbines
            + c.condensers + c.pumps + c.feedwater + c.accumulators + c.pipes;
        return c;
    }

    // ------------------------------------------------------------------ plans

    /** Plan the 1:4 core: graphite stack, fuel channels and control rods. */
    public static ConstructionJob planCore(ServerWorld world, BlockPos origin) {
        List<ConstructionJob.Region> regions = new ArrayList<>();
        int height = RbmkGeometry.STACK_HEIGHT_BLOCKS;
        int baseY = origin.getY();

        List<RbmkLattice.Cell> cells = RbmkLattice.graphiteCells();
        int[] gx = new int[cells.size()];
        int[] gz = new int[cells.size()];
        for (int i = 0; i < cells.size(); i++) {
            gx[i] = origin.getX() + cells.get(i).x();
            gz[i] = origin.getZ() + cells.get(i).z();
        }
        regions.add(ConstructionJob.columns(ConstructionJob.Part.GRAPHITE,
            ModBlocks.GRAPHITE_BLOCK, gx, gz, baseY, height));

        List<RbmkLattice.Channel> channels = RbmkLattice.channels();
        List<Integer> fx = new ArrayList<>(), fz = new ArrayList<>();
        List<Integer> rx = new ArrayList<>(), rz = new ArrayList<>();
        for (RbmkLattice.Channel ch : channels) {
            if (ch.control()) {
                rx.add(origin.getX() + ch.x());
                rz.add(origin.getZ() + ch.z());
            } else {
                fx.add(origin.getX() + ch.x());
                fz.add(origin.getZ() + ch.z());
            }
        }
        regions.add(ConstructionJob.columns(ConstructionJob.Part.FUEL,
            ModBlocks.FUEL_CHANNEL_BLOCK, toArray(fx), toArray(fz), baseY, height));
        regions.add(ConstructionJob.columns(ConstructionJob.Part.RODS,
            ModBlocks.CONTROL_ROD_BLOCK, toArray(rx), toArray(rz), baseY, height));

        return ConstructionJob.of(world, "RBMK core", regions);
    }

    /** Plan the plant machines around the core, as shells. */
    public static ConstructionJob planPlant(ServerWorld world, BlockPos origin) {
        List<ConstructionJob.Region> regions = new ArrayList<>();
        int y0 = origin.getY();
        Block sep = ModBlocks.STEAM_SEPARATOR_BLOCK;
        Block tur = ModBlocks.TURBINE_BLOCK;
        Block con = ModBlocks.CONDENSER_BLOCK;
        Block mcp = ModBlocks.MCP_BLOCK;
        Block feed = ModBlocks.FEEDWATER_BLOCK;
        Block eccs = ModBlocks.ECCS_ACCUMULATOR_BLOCK;

        // four steam separator drums: two rows of two, above and beside the core
        int sd = PlantLayout.SEPARATOR_DIAMETER_BLOCKS;
        int sl = PlantLayout.SEPARATOR_LENGTH_BLOCKS;
        int sh = PlantLayout.SEPARATOR_HEIGHT_BLOCKS;
        int off = PlantLayout.SEPARATOR_OFFSET_BLOCKS;
        for (int side = -1; side <= 1; side += 2) {
            for (int pair = 0; pair < 2; pair++) {
                int cx = origin.getX() + (pair == 0 ? -sl / 3 : sl / 3);
                int cz = origin.getZ() + side * off;
                regions.add(ConstructionJob.shell(ConstructionJob.Part.SEPARATORS, sep,
                    cx - sl / 2, y0 + sh, cz - sd / 2,
                    cx + sl / 2, y0 + sh + sd, cz + sd / 2));
            }
        }

        // two turbogenerators in the machine hall along +x
        int tl = PlantLayout.TURBINE_LENGTH_BLOCKS;
        int tw = PlantLayout.TURBINE_WIDTH_BLOCKS;
        int th = PlantLayout.TURBINE_HEIGHT_BLOCKS;
        int hallX = origin.getX() + PlantLayout.TURBINE_HALL_OFFSET_BLOCKS;
        for (int t = 0; t < 2; t++) {
            int cz = origin.getZ() + (t == 0 ? -tw : tw);
            regions.add(ConstructionJob.shell(ConstructionJob.Part.TURBINES, tur,
                hallX, y0, cz - tw / 2, hallX + tl, y0 + th, cz + tw / 2));
            int cl = PlantLayout.CONDENSER_LENGTH_BLOCKS;
            int cw = PlantLayout.CONDENSER_WIDTH_BLOCKS;
            int chh = PlantLayout.CONDENSER_HEIGHT_BLOCKS;
            regions.add(ConstructionJob.shell(ConstructionJob.Part.CONDENSERS, con,
                hallX, y0 - chh, cz - cw / 2, hallX + cl, y0, cz + cw / 2));
        }

        // eight main circulating pumps in a ring around the core
        int r = PlantLayout.MCP_RING_RADIUS_BLOCKS;
        int sz = PlantLayout.MCP_SIZE_BLOCKS;
        for (int i = 0; i < 8; i++) {
            double a = 2 * Math.PI * i / 8;
            int px = origin.getX() + (int) Math.round(r * Math.cos(a));
            int pz = origin.getZ() + (int) Math.round(r * Math.sin(a));
            regions.add(ConstructionJob.box(ConstructionJob.Part.PUMPS, mcp,
                px, y0, pz, px + sz, y0 + sz, pz + sz));
        }

        // four deaerators / feedwater tanks in a row
        int ds = PlantLayout.DEAERATOR_SIZE_BLOCKS;
        int dh = PlantLayout.DEAERATOR_HEIGHT_BLOCKS;
        for (int i = 0; i < 4; i++) {
            int dx = origin.getX() - r - ds - i * (ds + 2);
            int dz = origin.getZ() - ds / 2;
            regions.add(ConstructionJob.shell(ConstructionJob.Part.FEEDWATER, feed,
                dx, y0, dz, dx + ds, y0 + dh, dz + ds));
        }

        // twelve ECCS accumulators in a row
        int as = PlantLayout.ACCUMULATOR_SIZE_BLOCKS;
        int ah = PlantLayout.ACCUMULATOR_HEIGHT_BLOCKS;
        for (int i = 0; i < 12; i++) {
            int ax = origin.getX() - r - as - i * (as + 2);
            int az = origin.getZ() + ds + 4;
            regions.add(ConstructionJob.box(ConstructionJob.Part.ACCUMULATORS, eccs,
                ax, y0, az, ax + as, y0 + ah, az + as));
        }

        return ConstructionJob.of(world, "plant equipment", regions);
    }

    // -------------------------------------------------------------- immediate

    /** Place the core in one go (used to measure the burst cost). */
    public static Counts placeCore(ServerWorld world, BlockPos origin) {
        ConstructionJob job = planCore(world, origin);
        job.fill(Long.MAX_VALUE);
        return counts(job);
    }

    /** Place the plant machines in one go (used to measure the burst cost). */
    public static Counts placePlant(ServerWorld world, BlockPos origin) {
        ConstructionJob job = planPlant(world, origin);
        job.fill(Long.MAX_VALUE);
        return counts(job);
    }

    private static int[] toArray(List<Integer> list) {
        int[] a = new int[list.size()];
        for (int i = 0; i < a.length; i++) {
            a[i] = list.get(i);
        }
        return a;
    }
}
