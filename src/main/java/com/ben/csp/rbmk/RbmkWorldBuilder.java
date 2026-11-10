package com.ben.csp.rbmk;

import com.ben.csp.block.ModBlocks;
import com.ben.csp.rbmk.plant.PlantLayout;
import net.minecraft.block.Block;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Places the RBMK core and the rest of the plant into the world as real blocks at
 * 1:4. The core is placed solid (one block per lattice cell); the plant machines
 * are placed as hollow shells, so their form is visible without writing millions
 * of blocks — a turbine hall at 1:4 is 156 blocks long.
 */
public final class RbmkWorldBuilder {

    private RbmkWorldBuilder() {}

    /** Block counts from a build. */
    public static final class Counts {
        public long graphite, fuel, rods, separators, turbines, condensers, pumps,
            feedwater, accumulators, pipes, total;
    }

    /** Place the 1:4 core (graphite stack, fuel channels, control rods) at origin. */
    public static Counts placeCore(ServerWorld world, BlockPos origin) {
        Counts c = new Counts();
        Block graphite = ModBlocks.GRAPHITE_BLOCK;
        Block fuel = ModBlocks.FUEL_CHANNEL_BLOCK;
        Block rod = ModBlocks.CONTROL_ROD_BLOCK;
        int height = RbmkGeometry.STACK_HEIGHT_BLOCKS;

        for (RbmkLattice.Cell cell : RbmkLattice.graphiteCells()) {
            int x = origin.getX() + cell.x();
            int z = origin.getZ() + cell.z();
            for (int y = 0; y < height; y++) {
                world.setBlockState(new BlockPos(x, origin.getY() + y, z),
                    graphite.getDefaultState(), 2);
                c.graphite++;
            }
        }
        for (RbmkLattice.Channel ch : RbmkLattice.channels()) {
            int x = origin.getX() + ch.x();
            int z = origin.getZ() + ch.z();
            Block block = ch.control() ? rod : fuel;
            for (int y = 0; y < height; y++) {
                world.setBlockState(new BlockPos(x, origin.getY() + y, z),
                    block.getDefaultState(), 2);
            }
            if (ch.control()) {
                c.rods += height;
            } else {
                c.fuel += height;
            }
        }
        c.total = c.graphite + c.fuel + c.rods;
        return c;
    }

    /** Place the plant machines around the core, as shells. */
    public static Counts placePlant(ServerWorld world, BlockPos origin) {
        Counts c = new Counts();
        int y0 = origin.getY();

        // four steam separator drums: two rows of two, above and beside the core
        int sd = PlantLayout.SEPARATOR_DIAMETER_BLOCKS;
        int sl = PlantLayout.SEPARATOR_LENGTH_BLOCKS;
        int sh = PlantLayout.SEPARATOR_HEIGHT_BLOCKS;
        int off = PlantLayout.SEPARATOR_OFFSET_BLOCKS;
        for (int side = -1; side <= 1; side += 2) {
            for (int pair = 0; pair < 2; pair++) {
                int cx = origin.getX() + (pair == 0 ? -sl / 3 : sl / 3);
                int cz = origin.getZ() + side * off;
                c.separators += shell(world, cx - sl / 2, y0 + sh, cz - sd / 2,
                    cx + sl / 2, y0 + sh + sd, cz + sd / 2,
                    ModBlocks.STEAM_SEPARATOR_BLOCK);
            }
        }

        // two turbogenerators in the machine hall along +x
        int tl = PlantLayout.TURBINE_LENGTH_BLOCKS;
        int tw = PlantLayout.TURBINE_WIDTH_BLOCKS;
        int th = PlantLayout.TURBINE_HEIGHT_BLOCKS;
        int hallX = origin.getX() + PlantLayout.TURBINE_HALL_OFFSET_BLOCKS;
        for (int t = 0; t < 2; t++) {
            int cz = origin.getZ() + (t == 0 ? -tw : tw);
            c.turbines += shell(world, hallX, y0, cz - tw / 2,
                hallX + tl, y0 + th, cz + tw / 2, ModBlocks.TURBINE_BLOCK);
            // condenser under each turbine
            int cl = PlantLayout.CONDENSER_LENGTH_BLOCKS;
            int cw = PlantLayout.CONDENSER_WIDTH_BLOCKS;
            int chh = PlantLayout.CONDENSER_HEIGHT_BLOCKS;
            c.condensers += shell(world, hallX, y0 - chh, cz - cw / 2,
                hallX + cl, y0, cz + cw / 2, ModBlocks.CONDENSER_BLOCK);
        }

        // eight main circulating pumps in a ring around the core
        int r = PlantLayout.MCP_RING_RADIUS_BLOCKS;
        int sz = PlantLayout.MCP_SIZE_BLOCKS;
        for (int i = 0; i < 8; i++) {
            double a = 2 * Math.PI * i / 8;
            int px = origin.getX() + (int) Math.round(r * Math.cos(a));
            int pz = origin.getZ() + (int) Math.round(r * Math.sin(a));
            c.pumps += box(world, px, y0, pz, px + sz, y0 + sz, pz + sz,
                ModBlocks.MCP_BLOCK);
        }

        // four deaerators / feedwater tanks in a row
        int ds = PlantLayout.DEAERATOR_SIZE_BLOCKS;
        int dh = PlantLayout.DEAERATOR_HEIGHT_BLOCKS;
        for (int i = 0; i < 4; i++) {
            int dx = origin.getX() - r - ds - i * (ds + 2);
            int dz = origin.getZ() - ds / 2;
            c.feedwater += shell(world, dx, y0, dz, dx + ds, y0 + dh, dz + ds,
                ModBlocks.FEEDWATER_BLOCK);
        }

        // twelve ECCS accumulators in a row
        int as = PlantLayout.ACCUMULATOR_SIZE_BLOCKS;
        int ah = PlantLayout.ACCUMULATOR_HEIGHT_BLOCKS;
        for (int i = 0; i < 12; i++) {
            int ax = origin.getX() - r - as - i * (as + 2);
            int az = origin.getZ() + ds + 4;
            c.accumulators += box(world, ax, y0, az, ax + as, y0 + ah, az + as,
                ModBlocks.ECCS_ACCUMULATOR_BLOCK);
        }

        c.total = c.separators + c.turbines + c.condensers + c.pumps
            + c.feedwater + c.accumulators + c.pipes;
        return c;
    }

    /** Place a solid box; returns the number of blocks. */
    private static long box(ServerWorld world, int x0, int y0, int z0,
                            int x1, int y1, int z1, Block block) {
        long n = 0;
        for (int x = x0; x < x1; x++) {
            for (int y = y0; y < y1; y++) {
                for (int z = z0; z < z1; z++) {
                    world.setBlockState(new BlockPos(x, y, z), block.getDefaultState(), 2);
                    n++;
                }
            }
        }
        return n;
    }

    /** Place only the surface of a box (a hollow shell); returns the block count. */
    private static long shell(ServerWorld world, int x0, int y0, int z0,
                              int x1, int y1, int z1, Block block) {
        long n = 0;
        for (int x = x0; x < x1; x++) {
            for (int y = y0; y < y1; y++) {
                for (int z = z0; z < z1; z++) {
                    boolean surface = x == x0 || x == x1 - 1
                        || y == y0 || y == y1 - 1
                        || z == z0 || z == z1 - 1;
                    if (surface) {
                        world.setBlockState(new BlockPos(x, y, z), block.getDefaultState(), 2);
                        n++;
                    }
                }
            }
        }
        return n;
    }
}
