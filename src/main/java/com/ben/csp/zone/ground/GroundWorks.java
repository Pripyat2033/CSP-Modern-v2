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
import net.minecraft.util.math.Box;

/**
 * Puts the zone on the ground in the world. The territory boundary and the
 * reserved roads and rail become real blocks; a facility appears as a marker when
 * it is set out, and grows into a building as the work is done on it.
 */
public final class GroundWorks {

    private GroundWorks() {}

    private static final int MARK_HEIGHT = 1;
    private static final int BUILDING_HEIGHT = 5;

    /**
     * Mark the territory boundary and its reserved ground on the ground. This is
     * a survey mark, not a building: a dashed line of white concrete with a post
     * at each corner, so a player sees a boundary and not a wall.
     */
    public static void drawTerritory(ServerWorld world, PrombazaTerritory territory) {
        // A survey, not a wall. A torch at each corner, and a thin flat line only for the
        // reserved ground (road spine and rail corridor). The boundary line itself is left
        // clear, so no row of blocks is thrown across the site.
        for (Footprint r : territory.reserved()) {
            outlineTop(world, r, Blocks.YELLOW_CARPET);
        }
        for (Route route : territory.routes()) {
            for (int i = 0; i < route.pointCount() - 1; i++) {
                drawLineTop(world, route.x(i), route.z(i), route.x(i + 1), route.z(i + 1),
                    Blocks.GRAY_CARPET);
            }
        }
        cornerTorch(world, territory.boundary().minX(), territory.boundary().minZ());
        cornerTorch(world, territory.boundary().maxX(), territory.boundary().minZ());
        cornerTorch(world, territory.boundary().minX(), territory.boundary().maxZ());
        cornerTorch(world, territory.boundary().maxX(), territory.boundary().maxZ());
    }

    /** Remove the boundary and reserved marks, leaving the ground as it was. */
    public static void clearTerritory(ServerWorld world, PrombazaTerritory territory) {
        for (Footprint r : territory.reserved()) {
            clearRegionTop(world, r, Blocks.YELLOW_CARPET);
        }
        for (Route route : territory.routes()) {
            for (int i = 0; i < route.pointCount() - 1; i++) {
                clearLineTop(world, route.x(i), route.z(i), route.x(i + 1), route.z(i + 1),
                    Blocks.GRAY_CARPET);
            }
        }
    }

    /** A bright, self-lit mark at a chosen corner, so the player can see it. */
    public static void showCorner(ServerWorld world, int x, int z) {
        set(world, x, z, Blocks.TORCH, 1);
    }

    /** Take the corner mark away again. */
    public static void hideCorner(ServerWorld world, int x, int z) {
        clearTop(world, x, z, Blocks.TORCH);
    }

    private static void cornerTorch(ServerWorld world, double x, double z) {
        set(world, (int) Math.floor(x), (int) Math.floor(z), Blocks.TORCH, 1);
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
            outlineTop(world, plot, Blocks.YELLOW_CARPET);
        }
    }

    /** Remove a facility's marker or building blocks. */
    public static void clearFacility(ServerWorld world, Facility f) {
        if (f.hasFootprint()) {
            clearMarkers(world, f.footprint());
        }
    }

    /** Remove a construction site's marker or building blocks. */
    public static void clearSite(ServerWorld world, ConstructionSite site) {
        clearMarkers(world, site.plot());
    }

    private static final net.minecraft.block.Block[] MARKER_BLOCKS = {
        Blocks.YELLOW_CONCRETE, Blocks.LIGHT_GRAY_CONCRETE, Blocks.WHITE_CONCRETE,
        Blocks.STONE, Blocks.OAK_PLANKS, Blocks.STONE_BRICK_SLAB,
        Blocks.YELLOW_CARPET, Blocks.WHITE_CARPET, Blocks.GRAY_CARPET, Blocks.TORCH,
    };

    private static void clearMarkers(ServerWorld world, Footprint plot) {
        int x0 = (int) Math.floor(plot.minX());
        int x1 = (int) Math.floor(plot.maxX());
        int z0 = (int) Math.floor(plot.minZ());
        int z1 = (int) Math.floor(plot.maxZ());
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                if (!world.getChunkManager().isChunkLoaded(x >> 4, z >> 4)) {
                    continue;
                }
                int ground = world.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE, x, z);
                for (int dy = 0; dy <= 6; dy++) {
                    BlockPos pos = new BlockPos(x, ground + dy, z);
                    net.minecraft.block.Block here = world.getBlockState(pos).getBlock();
                    for (net.minecraft.block.Block b : MARKER_BLOCKS) {
                        if (here == b) {
                            world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
                            break;
                        }
                    }
                }
            }
        }
    }

    /** Draw an enterprise's headquarters by how far it has got, so the first thing
     *  the player sees is the offices going up, not empty ground. */
    public static void drawHq(ServerWorld world, Footprint plot, int percent, String name) {
        if (percent <= 0) {
            return;
        }
        if (percent < 35) {
            outlineTop(world, plot, Blocks.YELLOW_CARPET);
        } else if (percent < 70) {
            fillFlat(world, plot, Blocks.STONE, 0);
        } else if (percent < 100) {
            fillFlat(world, plot, Blocks.STONE, 0);
            raise(world, plot, Blocks.OAK_PLANKS, 3);
        } else {
            fillFlat(world, plot, Blocks.LIGHT_GRAY_CONCRETE, 0);
            raise(world, plot, Blocks.WHITE_CONCRETE, BUILDING_HEIGHT);
            placeSign(world, (int) Math.floor(plot.minX()), (int) Math.floor(plot.minZ()),
                name, "HEAD OFFICE");
        }
    }

    /** Mark out a rectangle the player is choosing, so the corners can be seen. */
    public static void previewOutline(ServerWorld world, Footprint plot) {
        dashedOutlineTop(world, plot, Blocks.WHITE_CARPET, 2);
    }

    /** Draw a construction site's progress: a marker, a foundation, or a building. */
    public static void drawSite(ServerWorld world, ConstructionSite site) {
        Footprint plot = site.plot();
        BuildStage stage = site.stage();
        switch (stage) {
            case STAKED -> outlineTop(world, plot, Blocks.YELLOW_CARPET);
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

    private static void dashedOutline(ServerWorld world, Footprint plot, net.minecraft.block.Block block, int period) {
        int x0 = (int) Math.floor(plot.minX());
        int x1 = (int) Math.floor(plot.maxX());
        int z0 = (int) Math.floor(plot.minZ());
        int z1 = (int) Math.floor(plot.maxZ());
        for (int x = x0; x <= x1; x++) {
            if ((x - x0) % period == 0) {
                set(world, x, z0, block);
                set(world, x, z1, block);
            }
        }
        for (int z = z0; z <= z1; z++) {
            if ((z - z0) % period == 0) {
                set(world, x0, z, block);
                set(world, x1, z, block);
            }
        }
    }

    private static void clearOutline(ServerWorld world, Footprint plot, net.minecraft.block.Block block) {
        int x0 = (int) Math.floor(plot.minX());
        int x1 = (int) Math.floor(plot.maxX());
        int z0 = (int) Math.floor(plot.minZ());
        int z1 = (int) Math.floor(plot.maxZ());
        for (int x = x0; x <= x1; x++) {
            clearIf(world, x, z0, block);
            clearIf(world, x, z1, block);
        }
        for (int z = z0; z <= z1; z++) {
            clearIf(world, x0, z, block);
            clearIf(world, x1, z, block);
        }
    }

    private static void clearDashedOutline(ServerWorld world, Footprint plot, net.minecraft.block.Block block, int period) {
        int x0 = (int) Math.floor(plot.minX());
        int x1 = (int) Math.floor(plot.maxX());
        int z0 = (int) Math.floor(plot.minZ());
        int z1 = (int) Math.floor(plot.maxZ());
        for (int x = x0; x <= x1; x++) {
            if ((x - x0) % period == 0) {
                clearIf(world, x, z0, block);
                clearIf(world, x, z1, block);
            }
        }
        for (int z = z0; z <= z1; z++) {
            if ((z - z0) % period == 0) {
                clearIf(world, x0, z, block);
                clearIf(world, x1, z, block);
            }
        }
    }

    private static void dashedOutlineTop(ServerWorld world, Footprint plot,
                                         net.minecraft.block.Block block, int period) {
        int x0 = (int) Math.floor(plot.minX());
        int x1 = (int) Math.floor(plot.maxX());
        int z0 = (int) Math.floor(plot.minZ());
        int z1 = (int) Math.floor(plot.maxZ());
        for (int x = x0; x <= x1; x++) {
            if ((x - x0) % period == 0) {
                set(world, x, z0, block, 1);
                set(world, x, z1, block, 1);
            }
        }
        for (int z = z0; z <= z1; z++) {
            if ((z - z0) % period == 0) {
                set(world, x0, z, block, 1);
                set(world, x1, z, block, 1);
            }
        }
    }

    private static void clearDashedOutlineTop(ServerWorld world, Footprint plot,
                                              net.minecraft.block.Block block, int period) {
        int x0 = (int) Math.floor(plot.minX());
        int x1 = (int) Math.floor(plot.maxX());
        int z0 = (int) Math.floor(plot.minZ());
        int z1 = (int) Math.floor(plot.maxZ());
        for (int x = x0; x <= x1; x++) {
            if ((x - x0) % period == 0) {
                clearIfTop(world, x, z0, block);
                clearIfTop(world, x, z1, block);
            }
        }
        for (int z = z0; z <= z1; z++) {
            if ((z - z0) % period == 0) {
                clearIfTop(world, x0, z, block);
                clearIfTop(world, x1, z, block);
            }
        }
    }

    private static void clearIfTop(ServerWorld world, int x, int z, net.minecraft.block.Block block) {
        if (!world.getChunkManager().isChunkLoaded(x >> 4, z >> 4)) {
            return;
        }
        int ground = world.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE, x, z);
        BlockPos pos = new BlockPos(x, ground + 1, z);
        if (world.getBlockState(pos).getBlock() == block) {
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
        }
    }

    /** A sign on a post, so a building can be named. */
    public static void placeSign(ServerWorld world, int x, int z, String... lines) {
        if (!world.getChunkManager().isChunkLoaded(x >> 4, z >> 4)) {
            return;
        }
        int ground = world.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE, x, z);
        BlockPos pos = new BlockPos(x, ground + 1, z);
        world.setBlockState(pos, Blocks.OAK_SIGN.getDefaultState(), 3);
        if (world.getBlockEntity(pos) instanceof net.minecraft.block.entity.SignBlockEntity sign) {
            net.minecraft.block.entity.SignText text = sign.getFrontText();
            for (int i = 0; i < lines.length && i < 4; i++) {
                text = text.withMessage(i, net.minecraft.text.Text.literal(lines[i]));
            }
            sign.setText(text, true);
        }
    }

    private static void outlineTop(ServerWorld world, Footprint plot, net.minecraft.block.Block block) {
        int x0 = (int) Math.floor(plot.minX());
        int x1 = (int) Math.floor(plot.maxX());
        int z0 = (int) Math.floor(plot.minZ());
        int z1 = (int) Math.floor(plot.maxZ());
        for (int x = x0; x <= x1; x++) {
            set(world, x, z0, block, 1);
            set(world, x, z1, block, 1);
        }
        for (int z = z0; z <= z1; z++) {
            set(world, x0, z, block, 1);
            set(world, x1, z, block, 1);
        }
    }

    private static void drawLineTop(ServerWorld world, double xa, double za, double xb, double zb,
                                    net.minecraft.block.Block block) {
        double len = Math.hypot(xb - xa, zb - za);
        int steps = (int) Math.ceil(len);
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0 : (double) i / steps;
            set(world, (int) Math.round(xa + (xb - xa) * t),
                (int) Math.round(za + (zb - za) * t), block, 1);
        }
    }

    private static void clearTop(ServerWorld world, int x, int z, net.minecraft.block.Block block) {
        if (!world.getChunkManager().isChunkLoaded(x >> 4, z >> 4)) {
            return;
        }
        int ground = world.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE, x, z);
        BlockPos pos = new BlockPos(x, ground + 1, z);
        if (world.getBlockState(pos).getBlock() == block) {
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
        }
    }

    private static void clearRegionTop(ServerWorld world, Footprint plot, net.minecraft.block.Block block) {
        int x0 = (int) Math.floor(plot.minX());
        int x1 = (int) Math.floor(plot.maxX());
        int z0 = (int) Math.floor(plot.minZ());
        int z1 = (int) Math.floor(plot.maxZ());
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                clearTop(world, x, z, block);
            }
        }
    }

    private static void clearLineTop(ServerWorld world, double xa, double za, double xb, double zb,
                                     net.minecraft.block.Block block) {
        double len = Math.hypot(xb - xa, zb - za);
        int steps = (int) Math.ceil(len);
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0 : (double) i / steps;
            clearTop(world, (int) Math.round(xa + (xb - xa) * t),
                (int) Math.round(za + (zb - za) * t), block);
        }
    }

    private static void clearIf(ServerWorld world, int x, int z, net.minecraft.block.Block block) {
        if (!world.getChunkManager().isChunkLoaded(x >> 4, z >> 4)) {
            return;
        }
        int ground = world.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE, x, z);
        BlockPos pos = new BlockPos(x, ground, z);
        if (world.getBlockState(pos).getBlock() == block) {
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
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
        // Never reach into an unloaded chunk: doing so loads it, and drawing a
        // large area would load the whole area at once and freeze the server.
        if (!world.getChunkManager().isChunkLoaded(x >> 4, z >> 4)) {
            return;
        }
        int ground = world.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE, x, z);
        BlockPos pos = new BlockPos(x, ground + dy, z);
        // Never build over a worker or a player: drawing a marker where someone
        // stands would bury them.
        if (!world.getOtherEntities(null, new Box(pos)).isEmpty()) {
            return;
        }
        BlockState state = block.getDefaultState();
        if (!world.getBlockState(pos).equals(state)) {
            world.setBlockState(pos, state, 3);
        }
    }
}
