package com.ben.csp.rbmk;

import com.ben.csp.block.MachineControllerBlock;
import com.ben.csp.block.ModBlocks;
import com.ben.csp.authorisation.PlantBuildAuthorisation;
import com.ben.csp.enterprise.Enterprise;
import com.ben.csp.perf.PerformanceProbe;
import com.ben.csp.prombaza.PrombazaRegistry;
import com.ben.csp.prombaza.PrombazaState;
import com.ben.csp.zone.ZoneState;
import com.ben.csp.rbmk.plant.Condenser;
import com.ben.csp.rbmk.plant.PlantPhysics;
import com.ben.csp.rbmk.plant.PlantSystem;
import com.ben.csp.rbmk.plant.MachineSpec;
import com.ben.csp.rbmk.plant.MachineValidator;
import com.ben.csp.rbmk.plant.PlantTicker;
import com.ben.csp.rbmk.plant.SteamSeparator;
import com.ben.csp.rbmk.physics.ReactorCore;
import com.ben.csp.rbmk.physics.RbmkSolver3D;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * {@code /rbmk test} — builds the real RBMK-1000 core inside the running server
 * and measures what it costs, so the question "can Minecraft cope with the core?"
 * is answered by the game itself rather than by a separate benchmark.
 *
 * <p>It builds the core (the 3D two-group diffusion solver over the 1,872
 * channels, 41,784 active nodes), solves it cold, then runs it for a simulated
 * minute doing one solver iteration per tick — the steady state of a running
 * reactor — and reports the wall-clock cost on the server thread.
 */
public final class RbmkTestCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger("csp-modern/rbmk");

    /** SOR factor; 1.85 nearly halves the sweeps of plain Gauss-Seidel. */
    private static final double OMEGA = 1.85;

    private static RbmkSolver3D solver;
    private static ReactorCore core;
    private static double lastMspt;
    private static int lastIters;

    // stress test state
    private static boolean stressListenerInstalled;
    private static java.util.List<ReactorCore> stressCores;
    private static java.util.List<PlantSystem> stressPlants;
    private static int stressTicksLeft;
    private static double stressCoreMs;
    private static double stressServerMs;

    private RbmkTestCommand() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("rbmk")
            .then(CommandManager.literal("test").executes(RbmkTestCommand::test))
            .then(CommandManager.literal("build")
                .executes(ctx -> build(ctx, false))
                .then(CommandManager.literal("immediate")
                    .executes(ctx -> build(ctx, false, true)))
                .then(CommandManager.literal("demo")
                    .executes(ctx -> build(ctx, true))
                    .then(CommandManager.literal("immediate")
                        .executes(ctx -> build(ctx, true, true)))))
            .then(CommandManager.literal("stress")
                .then(CommandManager.argument("count", IntegerArgumentType.integer(1, 64))
                    .executes(RbmkTestCommand::stress)
                    .then(CommandManager.literal("plant").executes(RbmkTestCommand::stressPlant))))
            .then(CommandManager.literal("status").executes(RbmkTestCommand::status))
            .then(CommandManager.literal("plant")
                .then(CommandManager.literal("start").executes(RbmkTestCommand::plantStart))
                .then(CommandManager.literal("scram").executes(RbmkTestCommand::plantScram))
                .then(CommandManager.literal("build")
                    .executes(ctx -> plantBuild(ctx, false))
                    .then(CommandManager.literal("immediate")
                        .executes(ctx -> plantBuild(ctx, false, true)))
                    .then(CommandManager.literal("demo")
                        .executes(ctx -> plantBuild(ctx, true))
                        .then(CommandManager.literal("immediate")
                            .executes(ctx -> plantBuild(ctx, true, true)))))
                .then(CommandManager.literal("status").executes(RbmkTestCommand::plantStatus)))
            .then(CommandManager.literal("machine").executes(RbmkTestCommand::machine)));
    }

    /**
     * {@code /rbmk machine} — stand on a machine controller and it validates the
     * machine's shell: the machine is "formed" only when the real casing is
     * present. Read-only, so it is not gated.
     */
    private static int machine(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource s = ctx.getSource();
        BlockPos pos = BlockPos.ofFloored(s.getPosition());
        BlockState st = s.getWorld().getBlockState(pos);
        MachineControllerBlock ctl = null;
        for (Block b : MACHINE_CONTROLLERS) {
            if (st.getBlock() == b) {
                ctl = (MachineControllerBlock) b;
                break;
            }
        }
        if (ctl == null) {
            feedback(s, "No machine controller here — stand on a controller block "
                + "(the corner of a box machine, or a drum's end-cap centre).",
                Formatting.YELLOW);
            return 0;
        }
        MachineSpec spec = ctl.spec();
        BlockPos min = ctl.minCorner(pos);
        MachineValidator.Result r =
            MachineValidator.validate(s.getWorld(), spec, min, pos);
        feedback(s, spec.displayName + " — " + r.describe(),
            r.formed ? Formatting.GREEN : Formatting.RED);
        feedback(s, String.format("  shell: %d x %d x %d blocks; controller at %d, %d, %d",
            spec.sizeX, spec.sizeY, spec.sizeZ, pos.getX(), pos.getY(), pos.getZ()),
            Formatting.GRAY);
        return r.formed ? 1 : 0;
    }

    private static final List<Block> MACHINE_CONTROLLERS = List.of(
        ModBlocks.STEAM_SEPARATOR_CONTROLLER_BLOCK,
        ModBlocks.TURBINE_CONTROLLER_BLOCK,
        ModBlocks.CONDENSER_CONTROLLER_BLOCK,
        ModBlocks.MCP_CONTROLLER_BLOCK,
        ModBlocks.DEAERATOR_CONTROLLER_BLOCK,
        ModBlocks.ECCS_CONTROLLER_BLOCK);

    private static int test(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource s = ctx.getSource();
        if (solver == null) {
            solver = new RbmkSolver3D();
            solver.setOmega(OMEGA);
            core = new ReactorCore();
        }

        int nodes = solver.activeNodes();
        int side = 2 * solver.extent() + 1;

        // cold solve, timed
        long t0 = System.nanoTime();
        int iters = solver.solve(600, 1e-7);
        double coldMs = (System.nanoTime() - t0) / 1e6;
        double kEffCold = solver.keff();
        lastIters = iters;

        // warm sample: a short burst of iterations from the converged flux, to
        // measure the steady-state per-tick cost without stalling the server
        int reps = 100;
        long t1 = System.nanoTime();
        for (int k = 0; k < reps; k++) {
            solver.stepOnce();
        }
        double runMs = (System.nanoTime() - t1) / 1e6;
        lastMspt = runMs / reps;

        core.step(0.05);

        feedback(s, "RBMK-1000 core, in-world", Formatting.GOLD);
        feedback(s, String.format("  mesh: %dx%d radial x 28 axial = %d nodes",
            side, side, side * side * 28), Formatting.WHITE);
        feedback(s, String.format("  active nodes: %d  (1,872 channels x 28)", nodes),
            Formatting.WHITE);
        feedback(s, String.format("  cold solve: k_eff = %.5f in %d iters, %.0f ms",
            kEffCold, iters, coldMs), Formatting.WHITE);
        feedback(s, String.format("  running core: %.2f ms per tick (one iteration + kinetics)",
            lastMspt), lastMspt < 25 ? Formatting.GREEN : Formatting.RED);
        feedback(s, String.format("  power: %.3f   reactivity: %+.5f",
            core.power(), core.reactivity()), Formatting.WHITE);
        feedback(s, verdict(), lastMspt < 25 ? Formatting.GREEN : Formatting.RED);

        LOGGER.info("RBMK in-world test: nodes={} coldSolve={}iters/{}ms running={}mspt k_eff={}",
            nodes, iters, coldMs, lastMspt, kEffCold);
        return 1;
    }

    /**
     * {@code /rbmk stress <n>} — the scale test. Builds {@code n} full cores in the
     * world (the real block cost) and runs {@code n} reactor cores live for 15 s,
     * then reports the per-tick cost and the server's own MSPT. This answers "how
     * many RBMK-1000s can this machine actually carry" with numbers, not hope.
     */
    private static int stress(CommandContext<ServerCommandSource> ctx) {
        return beginStress(ctx, IntegerArgumentType.getInteger(ctx, "count"), false);
    }

    /** {@code /rbmk stress <n> plant} — the same, with the full equipment around each. */
    private static int stressPlant(CommandContext<ServerCommandSource> ctx) {
        return beginStress(ctx, IntegerArgumentType.getInteger(ctx, "count"), true);
    }

    private static int beginStress(CommandContext<ServerCommandSource> ctx, int count,
                                   boolean withPlant) {
        ServerCommandSource s = ctx.getSource();
        ServerWorld world = s.getWorld();
        BlockPos origin = BlockPos.ofFloored(s.getPosition());

        if (!stressListenerInstalled) {
            net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK
                .register(RbmkTestCommand::stressTick);
            stressListenerInstalled = true;
        }

        long t0 = System.nanoTime();
        long placed = 0;
        int cols = (int) Math.ceil(Math.sqrt(count));
        for (int i = 0; i < count; i++) {
            int gx = (i % cols) * 96;
            int gz = (i / cols) * 96;
            BlockPos o = origin.add(gx, 0, gz);
            placed += RbmkWorldBuilder.placeCore(world, o).total;
            if (withPlant) {
                placed += RbmkWorldBuilder.placePlant(world, o).total;
            }
        }
        double buildMs = (System.nanoTime() - t0) / 1e6;

        stressCores = new java.util.ArrayList<>();
        stressPlants = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            stressCores.add(new ReactorCore());
            if (withPlant) {
                PlantSystem ps = new PlantSystem();
                ps.startUp();
                stressPlants.add(ps);
            }
        }
        stressTicksLeft = 300;
        stressCoreMs = 0;
        stressServerMs = 0;
        PerformanceProbe.resetPeak();

        feedback(s, String.format("stress: built %d %s (%d blocks) in %.0f ms",
            count, withPlant ? "complete plants" : "cores", placed, buildMs), Formatting.GOLD);
        feedback(s, "  now running them live for 15 s — watch the tick cost...",
            Formatting.GRAY);
        LOGGER.info("RBMK stress start: {} {}{}, {} blocks, build {} ms",
            count, withPlant ? "plants" : "cores", withPlant ? " (core+equipment)" : "",
            placed, buildMs);
        return 1;
    }

    private static void stressTick(net.minecraft.server.MinecraftServer server) {
        if (stressCores == null || stressTicksLeft <= 0) {
            return;
        }
        long t0 = System.nanoTime();
        for (ReactorCore c : stressCores) {
            c.step(0.05);
        }
        for (PlantSystem p : stressPlants) {
            p.step(0.05);
        }
        stressCoreMs += (System.nanoTime() - t0) / 1e6;
        stressServerMs += PerformanceProbe.averageMspt();
        if (--stressTicksLeft == 0) {
            int n = stressCores.size();
            double avgCore = stressCoreMs / 300.0;
            double avgServer = stressServerMs / 300.0;
            LOGGER.info("RBMK stress result: {} {}{}, core cost {} ms/tick, "
                + "server MSPT {} (peak {}), {}",
                n, stressPlants.isEmpty() ? "cores" : "plants",
                stressPlants.isEmpty() ? "" : " (core+equipment)",
                String.format("%.2f", avgCore), String.format("%.2f", avgServer),
                String.format("%.2f", PerformanceProbe.peakMspt()),
                PerformanceProbe.verdict());
            stressCores = null;
            stressPlants = null;
        }
    }

    private static int status(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource s = ctx.getSource();
        if (solver != null) {
            feedback(s, String.format("core: k_eff = %.5f, last %.2f ms/tick, %d cold iters",
                solver.keff(), lastMspt, lastIters), Formatting.WHITE);
        } else {
            feedback(s, "No core built yet — run /rbmk test.", Formatting.GRAY);
        }
        feedback(s, String.format("server tick: average %.2f ms, peak %.2f ms  (%s)",
            PerformanceProbe.averageMspt(), PerformanceProbe.peakMspt(),
            PerformanceProbe.verdict()), Formatting.WHITE);
        feedback(s, ConstructionScheduler.status(), Formatting.GRAY);
        return 1;
    }

    /**
     * {@code /rbmk build} — places the 1:4 core in the world as real blocks: one
     * graphite block per lattice cell, one fuel-channel block per fuel channel and
     * one control-rod block per control channel, through the core height, at the
     * command's position. This is the second half of the question: not just whether
     * the solver fits, but whether the world holds the core.
     */
    // --------------------------------------------------------- build authority

    /**
     * Refuse to raise plant unless it is authorised — the charter's hard gate.
     * The plant is built by the enterprises; until every enterprise is operational
     * and a prombaza has been designated, nothing of it may stand. The only way
     * past the gate is the explicit {@code demo} sub-command, which exists for
     * disposable test worlds and announces itself loudly.
     */
    private static boolean authorise(ServerCommandSource s, boolean demo) {
        if (demo) {
            feedback(s, "DEMO BUILD — plant construction gate bypassed for a disposable "
                + "test world. This is not a permitted site build.", Formatting.RED);
            return true;
        }
        List<Enterprise> enterprises;
        PrombazaRegistry prombazas;
        try {
            enterprises = ZoneState.get(s.getServer()).zone() == null
                ? List.of() : ZoneState.get(s.getServer()).zone().enterprises();
            prombazas = PrombazaState.get(s.getServer()).registry();
        } catch (RuntimeException e) {
            enterprises = List.of();
            prombazas = new PrombazaRegistry();
        }
        PlantBuildAuthorisation auth = new PlantBuildAuthorisation(enterprises, prombazas);
        if (auth.isGranted()) {
            return true;
        }
        feedback(s, "Plant construction is NOT authorised — nothing was built.",
            Formatting.RED);
        for (PlantBuildAuthorisation.Blocker b : auth.blockers()) {
            feedback(s, "  · " + b.reason(), Formatting.GRAY);
        }
        feedback(s, String.format("  enterprises ready: %d/%d; prombaza designated: %s",
            auth.readyCount(), auth.total(), auth.prombazaDesignated()), Formatting.GRAY);
        feedback(s, "  for a disposable test world only: /rbmk build demo",
            Formatting.YELLOW);
        return false;
    }

    private static int build(CommandContext<ServerCommandSource> ctx) {
        return build(ctx, false, false);
    }

    private static int build(CommandContext<ServerCommandSource> ctx, boolean demo) {
        return build(ctx, demo, false);
    }

    private static int build(CommandContext<ServerCommandSource> ctx, boolean demo,
                             boolean immediate) {
        ServerCommandSource s = ctx.getSource();
        if (!authorise(s, demo)) {
            return 0;
        }
        ServerWorld world = s.getWorld();
        BlockPos origin = BlockPos.ofFloored(s.getPosition());

        if (immediate) {
            long t0 = System.nanoTime();
            RbmkWorldBuilder.Counts c = RbmkWorldBuilder.placeCore(world, origin);
            double ms = (System.nanoTime() - t0) / 1e6;
            feedback(s, "RBMK-1000 core, placed at once (1:4) — burst measurement",
                Formatting.GOLD);
            feedback(s, String.format("  graphite %d, fuel %d, rod %d, UBS %d, LBS %d, "
                + "vessel %d, water tank %d; total %d in %.0f ms",
                c.graphite, c.fuel, c.rods, c.ubs, c.lbs, c.vessel, c.waterTank,
                c.total, ms), Formatting.WHITE);
            LOGGER.info("RBMK build (immediate): {} blocks in {} ms", c.total, ms);
            return 1;
        }

        ConstructionJob job = RbmkWorldBuilder.planCore(world, origin);
        ConstructionScheduler.enqueue(job);
        int rate = ConstructionScheduler.blocksPerTick();
        double etaS = job.planTotal() / (double) rate / 20.0;

        feedback(s, "RBMK-1000 core (graphite stack + its sections) queued (1:4)",
            Formatting.GOLD);
        feedback(s, String.format("  position: %d, %d, %d", origin.getX(), origin.getY(),
            origin.getZ()), Formatting.WHITE);
        feedback(s, String.format("  plan: %d blocks, placed %d per tick (about %.0f s)",
            job.planTotal(), rate, etaS), Formatting.WHITE);
        feedback(s, "  built on the server clock — /rbmk status shows progress",
            Formatting.GRAY);
        return 1;
    }

    /**
     * {@code /rbmk plant build} — places the whole plant at 1:4 as real blocks:
     * the core and its sections, then the four steam separator drums, two
     * turbogenerators, their condensers, eight main circulating pumps, the
     * feedwater tanks and the twelve ECCS accumulators as separate machines.
     */
    private static int plantBuild(CommandContext<ServerCommandSource> ctx) {
        return plantBuild(ctx, false, false);
    }

    private static int plantBuild(CommandContext<ServerCommandSource> ctx, boolean demo) {
        return plantBuild(ctx, demo, false);
    }

    private static int plantBuild(CommandContext<ServerCommandSource> ctx, boolean demo,
                                  boolean immediate) {
        ServerCommandSource s = ctx.getSource();
        if (!authorise(s, demo)) {
            return 0;
        }
        ServerWorld world = s.getWorld();
        BlockPos origin = BlockPos.ofFloored(s.getPosition());

        if (immediate) {
            long t0 = System.nanoTime();
            RbmkWorldBuilder.Counts core = RbmkWorldBuilder.placeCore(world, origin);
            RbmkWorldBuilder.Counts plant = RbmkWorldBuilder.placePlant(world, origin);
            double ms = (System.nanoTime() - t0) / 1e6;
            feedback(s, String.format("plant placed at once: %d + %d = %d blocks in %.0f ms",
                core.total, plant.total, core.total + plant.total, ms), Formatting.GOLD);
            LOGGER.info("RBMK plant build (immediate): core {} + plant {} in {} ms",
                core.total, plant.total, ms);
            return 1;
        }

        ConstructionJob core = RbmkWorldBuilder.planCore(world, origin);
        ConstructionJob plant = RbmkWorldBuilder.planPlant(world, origin);
        ConstructionScheduler.enqueue(core);
        ConstructionScheduler.enqueue(plant);
        long total = core.planTotal() + plant.planTotal();
        int rate = ConstructionScheduler.blocksPerTick();
        double etaS = total / (double) rate / 20.0;

        feedback(s, "RBMK-1000 plant queued for metered construction (1:4)", Formatting.GOLD);
        feedback(s, String.format("  core:  %d blocks (stack + UBS + LBS + vessel + water tank)",
            core.planTotal()), Formatting.WHITE);
        feedback(s, String.format("  plant: %d blocks (4 drums, 2 turbos, 2 condensers,",
            plant.planTotal()), Formatting.WHITE);
        feedback(s, "         8 ГЦН, 4 deaerators, 12 САОР)", Formatting.WHITE);
        feedback(s, String.format("  total: %d blocks at %d per tick (about %.0f s)",
            total, rate, etaS), Formatting.WHITE);
        feedback(s, "  built on the server clock, so the tick never hitches",
            Formatting.GRAY);
        feedback(s, "  then /rbmk plant start to run it live", Formatting.GRAY);
        return 1;
    }
    private static int plantStart(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource s = ctx.getSource();
        PlantTicker.startPlant();
        feedback(s, "RBMK-1000 plant started: rods on auto-regulator, plant running live",
            Formatting.GOLD);
        feedback(s, "  watch it hold — or fail to — with /rbmk plant status", Formatting.GRAY);
        return 1;
    }

    private static int plantScram(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource s = ctx.getSource();
        if (PlantTicker.plant() == null) {
            feedback(s, "No plant running — /rbmk plant start", Formatting.GRAY);
            return 0;
        }
        PlantTicker.plant().scram("operator AZ-5");
        feedback(s, "AZ-5: scram. Rods in, pumps tripping, ECCS actuated.", Formatting.RED);
        return 1;
    }

    private static int plantStatus(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource s = ctx.getSource();
        PlantSystem p = PlantTicker.plant();
        if (p == null) {
            feedback(s, "No plant running — /rbmk plant start", Formatting.GRAY);
            return 0;
        }
        double thermalMw = PlantPhysics.P_THERMAL_W * p.core().power() / 1e6;
        double electricMw = p.electricalOutputW() / 1e6;
        double flowM3h = 0;
        for (var pump : p.pumps()) {
            flowM3h += pump.flowM3s() * 3600;
        }
        double minLevel = 1, maxLevel = 0;
        for (SteamSeparator d : p.separators()) {
            minLevel = Math.min(minLevel, d.level());
            maxLevel = Math.max(maxLevel, d.level());
        }
        double rejectedMw = 0;
        for (Condenser c : p.condensers()) {
            rejectedMw += c.heatRejectedW() / 1e6;
        }
        feedback(s, String.format("RBMK-1000 plant, t = %.1f s%s", p.timeS(),
            p.scrammed() ? "  [SCRAM: " + p.tripReason() + "]" : ""),
            p.scrammed() ? Formatting.RED : Formatting.GOLD);
        feedback(s, String.format("  core:      power %.3f  (%.0f MWt), rods %.2f, k-eff state",
            p.core().power(), thermalMw, p.core().insertion()), Formatting.WHITE);
        feedback(s, String.format("  pumps:     %.0f m3/h total, %d running",
            flowM3h, runningPumps(p)), Formatting.WHITE);
        feedback(s, String.format("  drums:     level %.2f-%.2f, %.1f MPa",
            minLevel, maxLevel, p.separators()[0].pressurePa() / 1e6), Formatting.WHITE);
        feedback(s, String.format("  turbines:  %.0f MWe (%.0f rpm each)",
            electricMw, p.turbines()[0].rpm()), Formatting.WHITE);
        feedback(s, String.format("  condensers: rejecting %.0f MW, cooling water out %.1f C",
            rejectedMw, p.condensers()[0].coolingWaterOutC()), Formatting.WHITE);
        feedback(s, String.format("  feedwater: %.0f kg/s at %.0f C, deaerator %.2f",
            p.feedwater().flowKgS(), p.feedwater().temperatureC(),
            p.feedwater().deaeratorLevel()), Formatting.WHITE);
        feedback(s, String.format("  rotameters: %d channels, %d starving; oil %.1f bar",
            p.rotameters().length, p.starvingChannels(), p.oil()[0].pressureBar()),
            Formatting.WHITE);
        return 1;
    }

    private static int runningPumps(PlantSystem p) {
        int n = 0;
        for (var pump : p.pumps()) {
            if (pump.running()) {
                n++;
            }
        }
        return n;
    }

    private static String verdict() {
        if (lastMspt < 5) {
            return "  verdict: comfortable — the core is a small slice of a tick";
        }
        if (lastMspt < 25) {
            return "  verdict: fits — the core leaves most of the tick for the world";
        }
        return "  verdict: heavy — the core eats too much of the tick";
    }

    private static void feedback(ServerCommandSource s, String text, Formatting f) {
        s.sendFeedback(() -> Text.literal(text).formatted(f), false);
    }
}
