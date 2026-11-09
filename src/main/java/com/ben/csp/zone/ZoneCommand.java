package com.ben.csp.zone;

import com.ben.csp.prombaza.Area;
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
            .then(CommandManager.literal("spawn")
                .executes(ctx -> spawn(ctx, 120))
                .then(CommandManager.argument("radius", IntegerArgumentType.integer(16, 512))
                    .executes(ctx -> spawn(ctx, IntegerArgumentType.getInteger(ctx, "radius")))))
            .then(CommandManager.literal("status").executes(ZoneCommand::status))
            .then(CommandManager.literal("draw").executes(ZoneCommand::draw)));
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
        Area area = Area.of(p.getX() - radius, p.getY(), p.getZ() - radius,
                            p.getX() + radius, p.getY() + 20, p.getZ() + radius);
        state.spawn(area);
        state.markDirty();
        GroundWorks.drawTerritory(world, state.territory());
        source.sendFeedback(() -> Text.literal(
            "Промбаза laid out: " + state.territory().boundary().width() + "x"
            + state.territory().boundary().height() + " m, "
            + state.zone().count() + " enterprises standing on it."), true);
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
                "  " + e.name() + " [" + e.specialty().label() + "] "
                + (e.isOperational() ? "operational" : "forming") + " — "
                + e.facilities().size() + " facilities"), false);
        }
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
        GroundWorks.drawTerritory(world, state.territory());
        for (EnterpriseLifecycle l : state.zone().lifecycles()) {
            for (var f : l.enterprise().facilities()) {
                GroundWorks.drawFacility(world, f);
            }
        }
        source.sendFeedback(() -> Text.literal("Zone redrawn on the ground."), false);
        return 1;
    }
}
