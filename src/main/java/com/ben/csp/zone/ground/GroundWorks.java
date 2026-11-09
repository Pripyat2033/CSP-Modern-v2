package com.ben.csp.zone.ground;

import com.ben.csp.enterprise.Facility;
import com.ben.csp.enterprise.Footprint;
import com.ben.csp.infra.Route;
import com.ben.csp.zone.BuildStage;
import com.ben.csp.zone.ConstructionSite;
import com.ben.csp.zone.PrombazaTerritory;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Puts the zone on the ground in the world. The territory boundary and the
 * reserved roads and rail become real blocks; a facility appears as a marker when
 * it is set out, and grows into a building as the work is done on it.
 */
public final class GroundWorks {

    private GroundWorks() {}

    private static final int MARK_HEIGHT = 1;
    private static final int BUILDING_HEIGHT = 5;

    /** Draw the territory boundary and its reserved roads and rail on the ground. */
    public static void drawTerritory(ServerWorld world, PrombazaTerritory territory) {
        drawOutline(world, territory.boundary(), Blocks.RED_CONCRETE);
        for (Footprint r : territory.reserved()) {
            fillFlat(world, r, Blocks.GRAY_CONCRETE, 0);
        }
        for (Route route : territory.routes()) {
            for (int i = 0; i < route.pointCount() - 1; i++) {
                drawLine(world, route.x(i), route.z(i), route.x(i + 1), route.z(i + 1),
                    Blocks.GRAVEL);
            }
        }
    }

    /** Mark a facility's plot and, if it stands, raise its building. */
    public static void drawFacility(ServerWorld world, Facility f) {
        if (!f.hasFootprint()) {
            return;
        }
        Footprint plot = f.footprint();
        if (f.isBuilt()) {
            fillFlat(world, plot, Blocks.LIGHT_GRAY_CONCRETE, 0);
            raise(world, plot, Blocks.WHITE_CONCRETE, BUILDING_HEIGHT);
        } else {
            drawOutline(world, plot, Blocks.YELLOW_CONCRETE);
        }
    }

    /** Draw a construction site's progress: a marker, a foundation, or a building. */
    public static void drawSite(ServerWorld world, ConstructionSite site) {
        Footprint plot = site.plot();
        BuildStage stage = site.stage();
        switch (stage) {
            case STAKED -> drawOutline(world, plot, Blocks.YELLOW_CONCRETE);
            case FOUNDATION -> fillFlat(world, plot, Blocks.STONE, 0);
            case STRUCTURE -> {
                fillFlat(world, plot, Blocks.STONE, 0);
                raise(world, plot, Blocks.OAK_PLANKS, 3);
            }
            case ROOFED -> {
                fillFlat(world, plot, Blocks.STONE, 0);
                raise(world, plot, Blocks.OAK_PLANKS, 5);
                fillFlat(world, plot, Blocks.STONE_BRICK_SLAB, 6);
            }
            case FINISHED -> {
                fillFlat(world, plot, Blocks.LIGHT_GRAY_CONCRETE, 0);
                raise(world, plot, Blocks.WHITE_CONCRETE, BUILDING_HEIGHT);
            }
        }
    }

    private static void drawOutline(ServerWorld world, Footprint plot, net.minecraft.block.Block edge) {
        int x0 = (int) Math.floor(plot.minX());
        int x1 = (int) Math.floor(plot.maxX());
        int z0 = (int) Math.floor(plot.minZ());
        int z1 = (int) Math.floor(plot.maxZ());
        for (int x = x0; x <= x1; x++) {
            set(world, x, z0, edge);
            set(world, x, z1, edge);
        }
        for (int z = z0; z <= z1; z++) {
            set(world, x0, z, edge);
            set(world, x1, z, edge);
        }
    }

    private static void fillFlat(ServerWorld world, Footprint plot, net.minecraft.block.Block block, int dy) {
        int x0 = (int) Math.floor(plot.minX());
        int x1 = (int) Math.floor(plot.maxX());
        int z0 = (int) Math.floor(plot.minZ());
        int z1 = (int) Math.floor(plot.maxZ());
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                set(world, x, z, block, dy);
            }
        }
    }

    private static void raise(ServerWorld world, Footprint plot, net.minecraft.block.Block block, int height) {
        for (int dy = 1; dy <= height; dy++) {
            fillFlat(world, plot, block, dy);
        }
    }

    private static void drawLine(ServerWorld world, double xa, double za, double xb, double zb,
                                 net.minecraft.block.Block block) {
        double len = Math.hypot(xb - xa, zb - za);
        int steps = (int) Math.ceil(len);
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0 : (double) i / steps;
            set(world, (int) Math.round(xa + (xb - xa) * t), (int) Math.round(za + (zb - za) * t), block);
        }
    }

    private static void set(ServerWorld world, int x, int z, net.minecraft.block.Block block) {
        set(world, x, z, block, 0);
    }

    private static void set(ServerWorld world, int x, int z, net.minecraft.block.Block block, int dy) {
        int ground = world.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE, x, z);
        BlockPos pos = new BlockPos(x, ground + dy, z);
        BlockState state = block.getDefaultState();
        if (!world.getBlockState(pos).equals(state)) {
            world.setBlockState(pos, state, 3);
        }
    }
}
