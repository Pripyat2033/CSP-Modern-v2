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
        ServerWorld world = source.getWorld();
        BlockPos p = BlockPos.ofFloored(source.getPosition());
        ZoneState state = ZoneState.get(source.getServer());
        if (state.isSpawned()) {
            source.sendError(Text.literal("The zone is already spawned. Use /zone draw or /zone status."));
            return 0;
        }
        // The territory is the Director's to lay out. If a prombaza has been
        // designated, the enterprises come onto that ground; otherwise fall back
        // to a radius around the caller, for a quick trial.
        Area area;
        var prombazas = com.ben.csp.prombaza.PrombazaState.get(source.getServer()).registry();
        if (!prombazas.isEmpty()) {
            var pb = prombazas.all().get(prombazas.all().size() - 1);
            area = pb.area();
            source.sendFeedback(() -> Text.literal(
                "Building on the designated prombaza '" + pb.id() + "'."), false);
        } else {
            area = Area.of(p.getX() - radius, p.getY(), p.getZ() - radius,
                           p.getX() + radius, p.getY() + 20, p.getZ() + radius);
            source.sendFeedback(() -> Text.literal(
                "No prombaza designated: laying out a " + (radius * 2)
                + " m territory around you. Designate one with /directorate designate"
                + " for the proper ground."), false);
        }
        try {
            state.spawn(area);
        } catch (IllegalStateException e) {
            source.sendError(Text.literal("Could not stand the enterprises up: "
                + e.getMessage()
                + ". The ten enterprises need room for ten offices and their facilities - "
                + "designate a larger prombaza, or run /zone spawn 120 to lay out ground "
                + "around you."));
            return 0;
        }
        state.markDirty();
        source.sendFeedback(() -> Text.literal(
            "Prombaza laid out: " + state.territory().boundary().width() + " x "
            + state.territory().boundary().height() + " m."), true);
        source.sendFeedback(() -> Text.literal(
            state.zone().count() + " construction enterprises have come onto the "
            + "territory. Each will build its own office, hire staff, then set out "
            + "and build its facilities — a marker first, then a building over time."), false);
        source.sendFeedback(() -> Text.literal(
            "Each enterprise first builds its office, then hires its staff, then "
            + "builds its facilities — watch the offices and buildings go up on the "
            + "ground. It runs on its own clock: one work-day every ten seconds, so "
            + "the first buildings appear within about a minute. /zone status shows "
            + "what each one is doing. To undo the ground marks: /zone clear."), false);
        return 1;
    }

    /** Put the whole workforce on the ground and report the count. */
    private static int muster(CommandContext<ServerCommandSource> ctx, int count) {
        ServerCommandSource source = ctx.getSource();
        ServerWorld world = source.getWorld();
        ZoneState state = ZoneState.get(source.getServer());
        if (!state.isSpawned()) {
            source.sendError(Text.literal("The zone has not been spawned. Use /zone spawn."));
            return 0;
        }
        BlockPos at = BlockPos.ofFloored(source.getPosition());
        PerformanceProbe.resetPeak();
        long started = System.nanoTime();
        int placed = Muster.everyone(world, state.zone(), at, count);
        long ms = (System.nanoTime() - started) / 1_000_000L;
        source.sendFeedback(() -> Text.literal("Muster: " + placed + " workers on the ground in "
            + ms + " ms. " + Muster.count(world) + " now standing. Watch the tick with /zone perf."), false);
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
        ZoneState state = ZoneState.get(source.getServer());
        if (!state.isSpawned()) {
            source.sendError(Text.literal("The zone has not been spawned. Use /zone spawn."));
            return 0;
        }
        ZoneEnterprises zone = state.zone();
        source.sendFeedback(() -> Text.literal(
            "Day " + state.day() + ": " + zone.operationalCount() + "/" + zone.count()
            + " enterprises operational."), false);
        for (EnterpriseLifecycle l : zone.lifecycles()) {
            var e = l.enterprise();
            source.sendFeedback(() -> Text.literal(
                "  " + e.name() + " [" + e.specialty().label() + "] — "
                + l.progressLabel()), false);
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
