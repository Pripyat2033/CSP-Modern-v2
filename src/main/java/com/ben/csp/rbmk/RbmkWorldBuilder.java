package com.ben.csp.rbmk;

import com.ben.csp.block.ModBlocks;
import com.ben.csp.rbmk.plant.MachineSpec;
import com.ben.csp.rbmk.plant.MachineSpecs;
import com.ben.csp.rbmk.plant.PlantLayout;
import net.minecraft.block.Block;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes the RBMK-1000 core and the rest of the plant as real blocks at 1:4.
 *
 * <p>The core is not just graphite: it is the graphite stack <em>inside its
 * sections</em> — the biological shields, the vessel and the side water tanks
 * (the "Schema", the factory-built sections craned together on site). The builder
 * places them together, because a bare graphite stack is not a reactor.
 *
 * <p>The geometry is expressed as {@link ConstructionJob} plans — a handful of
 * region descriptors — so the build can be either <em>metered</em> (drained a few
 * thousand blocks per tick by {@link ConstructionScheduler}, the normal path) or
 * <em>immediate</em> (filled in one go, used only to measure the burst cost).
 */
public final class RbmkWorldBuilder {

    private RbmkWorldBuilder() {}

    /** Block counts from a build. */
    public static final class Counts {
        public long graphite, fuel, rods, ubs, lbs, vessel, waterTank,
            separators, turbines, condensers, pumps, feedwater, accumulators, pipes,
            total;
    }

    private static Counts counts(ConstructionJob job) {
        Counts c = new Counts();
        c.graphite = job.placed(ConstructionJob.Part.GRAPHITE);
        c.fuel = job.placed(ConstructionJob.Part.FUEL);
        c.rods = job.placed(ConstructionJob.Part.RODS);
        c.ubs = job.placed(ConstructionJob.Part.UBS);
        c.lbs = job.placed(ConstructionJob.Part.LBS);
        c.vessel = job.placed(ConstructionJob.Part.VESSEL);
        c.waterTank = job.placed(ConstructionJob.Part.WATER_TANK);
        c.separators = job.placed(ConstructionJob.Part.SEPARATORS);
        c.turbines = job.placed(ConstructionJob.Part.TURBINES);
        c.condensers = job.placed(ConstructionJob.Part.CONDENSERS);
        c.pumps = job.placed(ConstructionJob.Part.PUMPS);
        c.feedwater = job.placed(ConstructionJob.Part.FEEDWATER);
        c.accumulators = job.placed(ConstructionJob.Part.ACCUMULATORS);
        c.pipes = job.placed(ConstructionJob.Part.PIPES);
        c.total = c.graphite + c.fuel + c.rods + c.ubs + c.lbs + c.vessel + c.waterTank
            + c.separators + c.turbines + c.condensers + c.pumps + c.feedwater
            + c.accumulators + c.pipes;
        return c;
    }

    // ------------------------------------------------------------------ plans

    /** Plan the 1:4 core: graphite stack, channels, and the reactor's sections. */
    public static ConstructionJob planCore(ServerWorld world, BlockPos origin) {
        List<ConstructionJob.Region> regions = new ArrayList<>();
        int height = RbmkGeometry.STACK_HEIGHT_BLOCKS;
        int baseY = origin.getY();
        int ox = origin.getX();
        int oz = origin.getZ();

        // --- the graphite stack, one column per cell, pierced by the channels ---
        List<RbmkLattice.Cell> cells = RbmkLattice.graphiteCells();
        int[] gx = new int[cells.size()];
        int[] gz = new int[cells.size()];
        for (int i = 0; i < cells.size(); i++) {
            gx[i] = ox + cells.get(i).x();
            gz[i] = oz + cells.get(i).z();
        }
        regions.add(ConstructionJob.columns(ConstructionJob.Part.GRAPHITE,
            ModBlocks.GRAPHITE_BLOCK, gx, gz, baseY, height));

        List<RbmkLattice.Channel> channels = RbmkLattice.channels();
        List<Integer> fx = new ArrayList<>(), fz = new ArrayList<>();
        List<Integer> rx = new ArrayList<>(), rz = new ArrayList<>();
        for (RbmkLattice.Channel ch : channels) {
            if (ch.control()) {
                rx.add(ox + ch.x());
                rz.add(oz + ch.z());
            } else {
                fx.add(ox + ch.x());
                fz.add(oz + ch.z());
            }
        }
        regions.add(ConstructionJob.columns(ConstructionJob.Part.FUEL,
            ModBlocks.FUEL_CHANNEL_BLOCK, toArray(fx), toArray(fz), baseY, height));
        regions.add(ConstructionJob.columns(ConstructionJob.Part.RODS,
            ModBlocks.CONTROL_ROD_BLOCK, toArray(rx), toArray(rz), baseY, height));

        // --- the sections (the "Schema"), placed around and over the stack ---
        int stackR = RbmkGeometry.STACK_DIAMETER_BLOCKS / 2;              // 28
        int lbsR = RbmkGeometry.LBS_DIAMETER_BLOCKS / 2;                 // 29
        int vesselInner = lbsR;                                          // 29
        int vesselOuter = vesselInner + 1;                               // 1-block shell
        int tankInner = (int) Math.round(RbmkGeometry.WATER_TANK_INNER_DIAMETER_M
            / RbmkGeometry.METRES_PER_BLOCK / 2);                        // ~33
        int tankOuter = (int) Math.round(RbmkGeometry.WATER_TANK_OUTER_DIAMETER_M
            / RbmkGeometry.METRES_PER_BLOCK / 2);                        // ~38
        int ubsR = RbmkGeometry.UBS_DIAMETER_BLOCKS / 2;                 // 34

        int top = baseY + height;
        // lower biological shield: a disc under the core
        regions.add(ConstructionJob.radial(ConstructionJob.Part.LBS, ModBlocks.LBS_BLOCK,
            ox, oz, 0, lbsR, baseY - RbmkGeometry.LBS_THICKNESS_BLOCKS, baseY));
        // vessel (кожух): a steel shell around the stack
        regions.add(ConstructionJob.radial(ConstructionJob.Part.VESSEL, ModBlocks.VESSEL_BLOCK,
            ox, oz, vesselInner, vesselOuter, baseY, top));
        // side water tank (16 compartments of gamma shield): an annulus around all
        regions.add(ConstructionJob.radial(ConstructionJob.Part.WATER_TANK, ModBlocks.WATER_TANK_BLOCK,
            ox, oz, tankInner, tankOuter,
            baseY - RbmkGeometry.LBS_THICKNESS_BLOCKS, top));
        // upper biological shield (Схема Е): the disc over the core
        regions.add(ConstructionJob.radial(ConstructionJob.Part.UBS, ModBlocks.UBS_BLOCK,
            ox, oz, 0, ubsR, top, top + RbmkGeometry.UBS_THICKNESS_BLOCKS));

        return ConstructionJob.of(world, "RBMK core", regions);
    }

    /** Plan the plant machines around the core, as real multiblocks. */
    public static ConstructionJob planPlant(ServerWorld world, BlockPos origin) {
        List<ConstructionJob.Region> regions = new ArrayList<>();
        int y0 = origin.getY();
        Block sepCtl = ModBlocks.STEAM_SEPARATOR_CONTROLLER_BLOCK;
        Block turCtl = ModBlocks.TURBINE_CONTROLLER_BLOCK;
        Block conCtl = ModBlocks.CONDENSER_CONTROLLER_BLOCK;
        Block mcpCtl = ModBlocks.MCP_CONTROLLER_BLOCK;
        Block deaCtl = ModBlocks.DEAERATOR_CONTROLLER_BLOCK;
        Block eccsCtl = ModBlocks.ECCS_CONTROLLER_BLOCK;

        // ---- steam separator drums: horizontal cylinders, axis along x ----
        // Four of them (2 per half), as pressure vessels, not boxes.
        int sl = PlantLayout.SEPARATOR_LENGTH_BLOCKS;   // 124
        int sr = (PlantLayout.SEPARATOR_DIAMETER_BLOCKS - 1) / 2; // radius 5
        int off = PlantLayout.SEPARATOR_OFFSET_BLOCKS;  // 40
        int sh = PlantLayout.SEPARATOR_HEIGHT_BLOCKS;   // 44
        for (int side = -1; side <= 1; side += 2) {
            for (int pair = 0; pair < 2; pair++) {
                int x0 = origin.getX() + (pair == 0 ? -sl / 2 - sl / 4 : sl / 4);
                int cz = origin.getZ() + side * off;
                int cy = y0 + sh;
                addMachine(regions, ConstructionJob.Part.SEPARATORS, MachineSpecs.STEAM_SEPARATOR,
                    x0, cy - sr, cz - sr, sepCtl);
            }
        }

        // ---- turbogenerators: five cylinder covers + the TVV-500 generator ----
        // Two per unit, on one shaft along x, in the machine hall.
        int hallX = origin.getX() + PlantLayout.TURBINE_HALL_OFFSET_BLOCKS;
        int hallGap = PlantLayout.TURBINE_HALL_GAP_BLOCKS;
        for (int t = 0; t < 2; t++) {
            int czc = origin.getZ() + (t == 0 ? -hallGap : hallGap);
            addMachine(regions, ConstructionJob.Part.TURBINES, MachineSpecs.TURBINE,
                hallX, y0, czc - MachineSpecs.TURBINE.sizeZ / 2, turCtl);
            // the condenser sits directly under the turbine
            addMachine(regions, ConstructionJob.Part.CONDENSERS, MachineSpecs.CONDENSER,
                hallX, y0 - MachineSpecs.CONDENSER.sizeY, czc - MachineSpecs.CONDENSER.sizeZ / 2,
                conCtl);
        }

        // ---- main circulating pumps (ГЦН): 8, on a ring around the core ----
        int r = PlantLayout.MCP_RING_RADIUS_BLOCKS;
        for (int i = 0; i < 8; i++) {
            double a = 2 * Math.PI * i / 8;
            int px = origin.getX() + (int) Math.round(r * Math.cos(a));
            int pz = origin.getZ() + (int) Math.round(r * Math.sin(a));
            addMachine(regions, ConstructionJob.Part.PUMPS, MachineSpecs.MCP,
                px, y0, pz, mcpCtl);
        }

        // ---- deaerators: 4, also the feedwater storage tanks ----
        int ds = PlantLayout.DEAERATOR_SIZE_BLOCKS;
        for (int i = 0; i < 4; i++) {
            int dx = origin.getX() - r - ds - i * (ds + 2);
            int dz = origin.getZ() - ds / 2;
            addMachine(regions, ConstructionJob.Part.FEEDWATER, MachineSpecs.DEAERATOR,
                dx, y0, dz, deaCtl);
        }

        // ---- ECCS accumulators (САОР): 12, in two groups of six ----
        int as = PlantLayout.ACCUMULATOR_SIZE_BLOCKS;
        for (int i = 0; i < 12; i++) {
            int ax = origin.getX() - r - as - i * (as + 2);
            int az = origin.getZ() + ds + 4;
            addMachine(regions, ConstructionJob.Part.ACCUMULATORS, MachineSpecs.ECCS_ACCUMULATOR,
                ax, y0, az, eccsCtl);
        }

        return ConstructionJob.of(world, "plant equipment", regions);
    }

    /**
     * Add a whole machine — one casing or a composite — from its {@link MachineSpec},
     * so the blocks the builder places are exactly the cells the validator checks.
     * The controller stands at the spec's controller offset, replacing one shell cell.
     */
    private static void addMachine(List<ConstructionJob.Region> regions,
                                   ConstructionJob.Part part, MachineSpec spec,
                                   int x0, int y0, int z0, Block controller) {
        if (spec.isComposite()) {
            for (MachineSpec.Part p : spec.parts) {
                regions.add(partRegion(part, p.casing, p.shape,
                    x0 + p.dx, y0 + p.dy, z0 + p.dz, p.sizeX, p.sizeY, p.sizeZ));
            }
        } else {
            regions.add(partRegion(part, spec.casing, spec.shape,
                x0, y0, z0, spec.sizeX, spec.sizeY, spec.sizeZ));
        }
        int[] off = spec.controllerOffset();
        regions.add(ConstructionJob.box(ConstructionJob.Part.CONTROLLERS, controller,
            x0 + off[0], y0 + off[1], z0 + off[2],
            x0 + off[0] + 1, y0 + off[1] + 1, z0 + off[2] + 1));
    }

    private static ConstructionJob.Region partRegion(ConstructionJob.Part part, Block casing,
                                                     MachineSpec.Shape shape,
                                                     int x0, int y0, int z0,
                                                     int sx, int sy, int sz) {
        if (shape == MachineSpec.Shape.CYLINDER_X) {
            int radius = (sy - 1) / 2;
            return ConstructionJob.cylinderX(part, casing, x0, x0 + sx,
                y0 + sy / 2, z0 + sz / 2, radius);
        }
        return ConstructionJob.shell(part, casing, x0, y0, z0, x0 + sx, y0 + sy, z0 + sz);
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
