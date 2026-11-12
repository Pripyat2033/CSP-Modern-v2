package com.ben.csp.rbmk;

import com.ben.csp.block.ModBlocks;
import com.ben.csp.rbmk.module.ModuleRegistry;
import com.ben.csp.rbmk.module.PlantCell;
import com.ben.csp.rbmk.module.PlantModule;
import com.ben.csp.rbmk.plant.Compartment;
import com.ben.csp.rbmk.plant.EnergoblokLayout;
import com.ben.csp.rbmk.plant.MachineSpec;
import com.ben.csp.rbmk.plant.MachineSpecs;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

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
            separators, turbines, condensers, pumps, feedwater, accumulators,
            pipes, valves, meters, containment, controllers, total;
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
        c.valves = job.placed(ConstructionJob.Part.VALVES);
        c.meters = job.placed(ConstructionJob.Part.METERS);
        c.containment = job.placed(ConstructionJob.Part.CONTAINMENT);
        c.controllers = job.placed(ConstructionJob.Part.CONTROLLERS);
        c.total = c.graphite + c.fuel + c.rods + c.ubs + c.lbs + c.vessel + c.waterTank
            + c.separators + c.turbines + c.condensers + c.pumps + c.feedwater
            + c.accumulators + c.pipes + c.valves + c.meters + c.containment
            + c.controllers;
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
        // upper biological shield (Схема Е): the disc over the core, PIERCED. It is
        // not a solid lid: every one of the 1,872 channels passes through it (each
        // channel has its own plug), and the rod drives and steam lines pass
        // through too. So the shield is built as the disc minus the channel cells.
        regions.add(ConstructionJob.piercedDisc(ConstructionJob.Part.UBS, ModBlocks.UBS_BLOCK,
            ox, oz, 0, ubsR, top, top + RbmkGeometry.UBS_THICKNESS_BLOCKS,
            channelX(channels, ox), channelZ(channels, oz)));

        return ConstructionJob.of(world, "RBMK core", regions);
    }

    private static int[] channelX(List<RbmkLattice.Channel> channels, int ox) {
        int[] a = new int[channels.size()];
        for (int i = 0; i < a.length; i++) {
            a[i] = ox + channels.get(i).x();
        }
        return a;
    }

    private static int[] channelZ(List<RbmkLattice.Channel> channels, int oz) {
        int[] a = new int[channels.size()];
        for (int i = 0; i < a.length; i++) {
            a[i] = oz + channels.get(i).z();
        }
        return a;
    }

    /**
     * Plan the plant as an <b>energoblok</b>: the sealed, pressure-resistant
     * concrete boxes that are the containment, each with its machines inside it.
     * The boxes come first, then every machine is placed from
     * {@link EnergoblokLayout#machines()} so nothing can drift out of its room.
     */
    public static ConstructionJob planPlant(ServerWorld world, BlockPos origin) {
        List<ConstructionJob.Region> regions = new ArrayList<>();
        int ox = origin.getX();
        int oy = origin.getY();
        int oz = origin.getZ();

        // ---- the boxes: walls (shells) around each sealed compartment ----
        for (Compartment c : EnergoblokLayout.compartments()) {
            int[] o = c.outerBox();
            regions.add(ConstructionJob.shell(ConstructionJob.Part.CONTAINMENT,
                ModBlocks.CONTAINMENT_WALL_BLOCK,
                ox + o[0], oy + o[1], oz + o[2],
                ox + o[3], oy + o[4], oz + o[5]));
        }

        // ---- the machines, each inside its room ----
        for (EnergoblokLayout.Placement p : EnergoblokLayout.machines()) {
            MachineSpec spec = specFor(p.part());
            if (spec == null) {
                continue;
            }
            Block ctl = controllerFor(p.part());
            addMachine(regions, p.part(), spec, ox + p.x(), oy + p.y(), oz + p.z(), ctl);
        }

        // ---- the swappable modules (the МЦК pipework today), composed in ----
        for (PlantModule m : ModuleRegistry.all()) {
            regions.addAll(moduleRegions(origin, m));
        }

        return ConstructionJob.of(world, "energoblok", regions);
    }

    // --------------------------------------------------------------- modules

    /**
     * Plan one swappable module as its own job. A module is pure data (origin-
     * relative cells with a material role), so this maps each role to its block
     * and returns a {@link ConstructionJob} the scheduler can build on its own.
     */
    public static ConstructionJob planModule(ServerWorld world, BlockPos origin, PlantModule m) {
        return ConstructionJob.of(world, "module:" + m.id(), moduleRegions(origin, m));
    }

    /** The regions a module contributes, grouped by material role. */
    private static List<ConstructionJob.Region> moduleRegions(BlockPos origin, PlantModule m) {
        List<ConstructionJob.Region> regions = new ArrayList<>();
        Map<PlantCell.Role, List<PlantCell>> byRole = new EnumMap<>(PlantCell.Role.class);
        for (PlantCell c : m.cells()) {
            byRole.computeIfAbsent(c.role(), k -> new ArrayList<>()).add(c);
        }
        for (Map.Entry<PlantCell.Role, List<PlantCell>> e : byRole.entrySet()) {
            List<PlantCell> list = e.getValue();
            int n = list.size();
            int[] xs = new int[n];
            int[] ys = new int[n];
            int[] zs = new int[n];
            for (int i = 0; i < n; i++) {
                PlantCell c = list.get(i);
                xs[i] = origin.getX() + c.x();
                ys[i] = origin.getY() + c.y();
                zs[i] = origin.getZ() + c.z();
            }
            regions.add(ConstructionJob.cells(partFor(e.getKey()), blockFor(e.getKey()),
                xs, ys, zs));
        }
        return regions;
    }

    /**
     * Remove a module: clear exactly its own cells, and nothing else. This is what
     * makes a module swappable — the audit ({@link ModuleRegistry#audit}) has
     * already proved the cells are disjoint from the core, the boxes and the
     * machines, so clearing them cannot touch anything the module does not own.
     */
    public static long clearModule(ServerWorld world, BlockPos origin, PlantModule m) {
        long n = 0;
        for (PlantCell c : m.cells()) {
            world.setBlockState(new BlockPos(origin.getX() + c.x(),
                origin.getY() + c.y(), origin.getZ() + c.z()),
                Blocks.AIR.getDefaultState(), 2);
            n++;
        }
        return n;
    }

    /** Place a whole module at once (the immediate path), returning its job. */
    public static ConstructionJob placeModule(ServerWorld world, BlockPos origin, PlantModule m) {
        ConstructionJob job = planModule(world, origin, m);
        job.fill(Long.MAX_VALUE);
        return job;
    }

    /** The counts of a finished job (for the command feedback). */
    public static Counts countsOf(ConstructionJob job) {
        return counts(job);
    }

    private static ConstructionJob.Part partFor(PlantCell.Role role) {
        return switch (role) {
            case VALVE -> ConstructionJob.Part.VALVES;
            case METER -> ConstructionJob.Part.METERS;
            default -> ConstructionJob.Part.PIPES;
        };
    }

    private static Block blockFor(PlantCell.Role role) {
        return switch (role) {
            case VALVE -> ModBlocks.PLANT_VALVE_BLOCK;
            case METER -> ModBlocks.PLANT_METER_BLOCK;
            default -> ModBlocks.PLANT_PIPE_BLOCK;
        };
    }

    private static MachineSpec specFor(ConstructionJob.Part part) {
        return switch (part) {
            case SEPARATORS -> MachineSpecs.STEAM_SEPARATOR;
            case TURBINES -> MachineSpecs.TURBINE;
            case CONDENSERS -> MachineSpecs.CONDENSER;
            case PUMPS -> MachineSpecs.MCP;
            case FEEDWATER -> MachineSpecs.DEAERATOR;
            case ACCUMULATORS -> MachineSpecs.ECCS_ACCUMULATOR;
            default -> null;
        };
    }

    private static Block controllerFor(ConstructionJob.Part part) {
        return switch (part) {
            case SEPARATORS -> ModBlocks.STEAM_SEPARATOR_CONTROLLER_BLOCK;
            case TURBINES -> ModBlocks.TURBINE_CONTROLLER_BLOCK;
            case CONDENSERS -> ModBlocks.CONDENSER_CONTROLLER_BLOCK;
            case PUMPS -> ModBlocks.MCP_CONTROLLER_BLOCK;
            case FEEDWATER -> ModBlocks.DEAERATOR_CONTROLLER_BLOCK;
            case ACCUMULATORS -> ModBlocks.ECCS_CONTROLLER_BLOCK;
            default -> ModBlocks.STEAM_SEPARATOR_CONTROLLER_BLOCK;
        };
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
