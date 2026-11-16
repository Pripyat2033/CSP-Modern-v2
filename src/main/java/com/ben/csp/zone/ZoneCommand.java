package com.ben.csp.zone;

import com.ben.csp.prombaza.Area;
import com.ben.csp.perf.PerformanceProbe;
import com.ben.csp.zone.ground.GroundWorks;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/**
 * Operator commands for the zone.
 *
 *   /zone spawn [radius]   lay out the prombaza territory and put the roster on it
 *   /zone status           report how far the enterprises have come
 *   /zone draw             redraw the territory and the facilities on the ground
 */
public final class ZoneCommand {

    private ZoneCommand() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("zone")
            .requires(source -> source.hasPermissionLevel(2))
            .executes(ctx -> { ctx.getSource().sendError(Text.literal(
                "Usage: /zone spawn [radius] | status | draw | clear  (see /csp help)")); return 0; })
            .then(CommandManager.literal("spawn")
                .executes(ctx -> spawn(ctx, 120))
                .then(CommandManager.argument("radius", IntegerArgumentType.integer(16, 512))
                    .executes(ctx -> spawn(ctx, IntegerArgumentType.getInteger(ctx, "radius")))))
            .then(CommandManager.literal("status").executes(ZoneCommand::status))
            .then(CommandManager.literal("muster")
                .executes(ctx -> muster(ctx, -1))
                .then(CommandManager.argument("count", IntegerArgumentType.integer(1, 2000))
                    .executes(ctx -> muster(ctx, IntegerArgumentType.getInteger(ctx, "count")))))
            .then(CommandManager.literal("demob").executes(ZoneCommand::demob))
            .then(CommandManager.literal("perf").executes(ZoneCommand::perf))
            .then(CommandManager.literal("draw").executes(ZoneCommand::draw))
            .then(CommandManager.literal("clear").executes(ZoneCommand::clear)));
    }

    private static int spawn(CommandContext<ServerCommandSource> ctx, int radius) {
        ServerCommandSource source = ctx.getSource();
        BlockPos p = BlockPos.ofFloored(source.getPosition());
        ZoneOperations.Result r = ZoneOperations.foundSite(
            source.getServer(), source.getWorld(), p, radius, source.getName());
        if (!r.ok()) {
            source.sendError(Text.literal(String.join(" ", r.lines())));
            return 0;
        }
        for (String line : r.lines()) {
            source.sendFeedback(() -> Text.literal(line), false);
        }
        return 1;
    }

    /** Put the whole workforce on the ground and report the count. */
    private static int muster(CommandContext<ServerCommandSource> ctx, int count) {
        ServerCommandSource source = ctx.getSource();
        BlockPos at = BlockPos.ofFloored(source.getPosition());
        ZoneOperations.Result r = ZoneOperations.muster(
            source.getServer(), source.getWorld(), at, count);
        if (!r.ok()) {
            source.sendError(Text.literal(String.join(" ", r.lines())));
            return 0;
        }
        for (String line : r.lines()) {
            source.sendFeedback(() -> Text.literal(line), false);
        }
        return 1;
    }

    /** Remove the workforce from the ground. */
    private static int demob(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        int removed = Muster.demobilise(source.getWorld());
        source.sendFeedback(() -> Text.literal("Demobilised " + removed + " workers."), false);
        return 1;
    }

    /** Report the server tick time — the plain answer to "can it cope?". */
    private static int perf(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        int workers = Muster.count(source.getWorld());
        double avg = PerformanceProbe.averageMspt();
        double peak = PerformanceProbe.peakMspt();
        source.sendFeedback(() -> Text.literal(String.format(
            "Tick: %.2f ms average, %.2f ms peak (budget 50 ms). %d workers. Verdict: %s.",
            avg, peak, workers, PerformanceProbe.verdict())), false);
        return 1;
    }

    private static int status(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        ZoneOperations.Result r = ZoneOperations.siteReport(source.getServer());
        if (!r.ok()) {
            source.sendError(Text.literal(String.join(" ", r.lines())));
            return 0;
        }
        for (String line : r.lines()) {
            source.sendFeedback(() -> Text.literal(line), false);
        }
        return 1;
    }

    private static int clear(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        ServerWorld world = source.getWorld();
        ZoneState state = ZoneState.get(source.getServer());
        if (!state.isSpawned()) {
            source.sendError(Text.literal("The zone has not been spawned."));
            return 0;
        }
        GroundWorks.clearTerritory(world, state.territory());
        for (EnterpriseLifecycle l : state.zone().lifecycles()) {
            for (var f : l.enterprise().facilities()) {
                GroundWorks.clearFacility(world, f);
            }
            for (ConstructionSite s : l.sites()) {
                GroundWorks.clearSite(world, s);
            }
        }
        source.sendFeedback(() -> Text.literal(
            "Ground marks cleared. The zone itself is untouched — /zone draw puts "
            + "the marks back."), true);
        return 1;
    }

    private static int draw(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        ServerWorld world = source.getWorld();
        ZoneState state = ZoneState.get(source.getServer());
        if (!state.isSpawned()) {
            source.sendError(Text.literal("The zone has not been spawned. Use /zone spawn."));
            return 0;
        }
        for (EnterpriseLifecycle l : state.zone().lifecycles()) {
            for (var f : l.enterprise().facilities()) {
                GroundWorks.drawFacility(world, f);
            }
        }
        source.sendFeedback(() -> Text.literal("Zone redrawn on the ground."), false);
        return 1;
    }
}
