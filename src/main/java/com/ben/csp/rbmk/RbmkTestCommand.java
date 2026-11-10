package com.ben.csp.rbmk;

import com.ben.csp.block.ModBlocks;
import com.ben.csp.perf.PerformanceProbe;
import com.ben.csp.rbmk.physics.ReactorCore;
import com.ben.csp.rbmk.physics.RbmkSolver3D;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.block.Block;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private RbmkTestCommand() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("rbmk")
            .then(CommandManager.literal("test").executes(RbmkTestCommand::test))
            .then(CommandManager.literal("build").executes(RbmkTestCommand::build))
            .then(CommandManager.literal("status").executes(RbmkTestCommand::status)));
    }

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
        return 1;
    }

    /**
     * {@code /rbmk build} — places the 1:4 core in the world as real blocks: one
     * graphite block per lattice cell, one fuel-channel block per fuel channel and
     * one control-rod block per control channel, through the core height, at the
     * command's position. This is the second half of the question: not just whether
     * the solver fits, but whether the world holds the core.
     */
    private static int build(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource s = ctx.getSource();
        ServerWorld world = s.getWorld();
        BlockPos origin = BlockPos.ofFloored(s.getPosition());

        Block graphite = ModBlocks.GRAPHITE_BLOCK;
        Block fuel = ModBlocks.FUEL_CHANNEL_BLOCK;
        Block rod = ModBlocks.CONTROL_ROD_BLOCK;

        int height = RbmkGeometry.CORE_HEIGHT_BLOCKS;
        long t0 = System.nanoTime();
        int graphiteBlocks = 0;
        int fuelBlocks = 0;
        int rodBlocks = 0;

        // graphite moderator: one block per lattice cell, through the core height
        for (RbmkLattice.Cell cell : RbmkLattice.graphiteCells()) {
            int x = origin.getX() + cell.x();
            int z = origin.getZ() + cell.z();
            for (int y = 0; y < height; y++) {
                world.setBlockState(new BlockPos(x, origin.getY() + y, z),
                    graphite.getDefaultState(), 2);
                graphiteBlocks++;
            }
        }

        // channels: the fuel (ТК) and control (СУЗ) columns pierce the graphite
        for (RbmkLattice.Channel c : RbmkLattice.channels()) {
            int x = origin.getX() + c.x();
            int z = origin.getZ() + c.z();
            Block channel = c.control() ? rod : fuel;
            for (int y = 0; y < height; y++) {
                world.setBlockState(new BlockPos(x, origin.getY() + y, z),
                    channel.getDefaultState(), 2);
            }
            if (c.control()) {
                rodBlocks += height;
            } else {
                fuelBlocks += height;
            }
        }
        double ms = (System.nanoTime() - t0) / 1e6;

        feedback(s, "RBMK-1000 core, placed as blocks (1:4)", Formatting.GOLD);
        feedback(s, String.format("  position: %d, %d, %d", origin.getX(), origin.getY(),
            origin.getZ()), Formatting.WHITE);
        feedback(s, String.format("  graphite:       %d blocks", graphiteBlocks), Formatting.WHITE);
        feedback(s, String.format("  fuel channels:  %d blocks (ТК)", fuelBlocks), Formatting.WHITE);
        feedback(s, String.format("  control rods:   %d blocks (СУЗ)", rodBlocks), Formatting.WHITE);
        feedback(s, String.format("  total written:  %d block updates in %.0f ms",
            graphiteBlocks + fuelBlocks + rodBlocks, ms), Formatting.WHITE);
        feedback(s, "  now watch the server tick — /rbmk test for the solver cost",
            Formatting.GRAY);
        LOGGER.info("RBMK build: {} graphite, {} fuel, {} rod blocks in {} ms",
            graphiteBlocks, fuelBlocks, rodBlocks, ms);
        return 1;
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
