package com.ben.csp.prombaza;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;
import com.ben.csp.zone.ground.GroundWorks;

/**
 * Operator commands to designate and undo a prombaza.
 *
 *   /prombaza pos1 [x y z]     set corner 1 (at the caller, or explicit coords)
 *   /prombaza pos2 [x y z]     set corner 2
 *   /prombaza designate <id> <name>
 *   /prombaza list
 *   /prombaza undo <id>
 *   /prombaza control <id>     show the входной контроль desk for a prombaza
 *   /prombaza yard <id>        show the rail siding (standing wagons and demurrage)
 *   /prombaza receive <id> <n> clear up to n standing wagons through the desk
 */
public final class PrombazaCommand {

    private PrombazaCommand() {}

    private static BlockPos pendingPos1;
    private static BlockPos pendingPos2;

    /** The corners set with /prombaza pos1 and pos2, for the directorate to use. */
    public static BlockPos pendingPos1() { return pendingPos1; }
    public static BlockPos pendingPos2() { return pendingPos2; }

    /** Clear the pending corners once a designation has been carried out. */
    public static void clearPendingCorners() {
        pendingPos1 = null;
        pendingPos2 = null;
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("prombaza")
            .requires(source -> source.hasPermissionLevel(2))
            .executes(ctx -> usage(ctx, "/prombaza pos1 | pos2 | list | designate <id> [name] | undo <id>"))
            .then(CommandManager.literal("pos1")
                .executes(ctx -> setPos(ctx, true, null))
                .then(CommandManager.argument("x", IntegerArgumentType.integer())
                    .then(CommandManager.argument("y", IntegerArgumentType.integer())
                        .then(CommandManager.argument("z", IntegerArgumentType.integer())
                            .executes(ctx -> setPos(ctx, true, readCoords(ctx)))))))
            .then(CommandManager.literal("pos2")
                .executes(ctx -> setPos(ctx, false, null))
                .then(CommandManager.argument("x", IntegerArgumentType.integer())
                    .then(CommandManager.argument("y", IntegerArgumentType.integer())
                        .then(CommandManager.argument("z", IntegerArgumentType.integer())
                            .executes(ctx -> setPos(ctx, false, readCoords(ctx)))))))
            .then(CommandManager.literal("designate")
                .then(CommandManager.argument("id", StringArgumentType.word())
                    .executes(ctx -> designate(ctx, StringArgumentType.getString(ctx, "id")))
                    .then(CommandManager.argument("name", StringArgumentType.greedyString())
                        .executes(ctx -> designate(ctx, StringArgumentType.getString(ctx, "name"))))))
            .then(CommandManager.literal("list").executes(PrombazaCommand::list))
            .then(CommandManager.literal("control")
                .then(CommandManager.argument("id", StringArgumentType.word())
                    .executes(PrombazaCommand::control)))
            .then(CommandManager.literal("yard")
                .then(CommandManager.argument("id", StringArgumentType.word())
                    .executes(PrombazaCommand::yard)))
            .then(CommandManager.literal("receive")
                .then(CommandManager.argument("id", StringArgumentType.word())
                    .then(CommandManager.argument("n", IntegerArgumentType.integer(1))
                        .executes(ctx -> receive(ctx, StringArgumentType.getString(ctx, "id"),
                            IntegerArgumentType.getInteger(ctx, "n"))))))
            .then(CommandManager.literal("undo")
                .executes(ctx -> usage(ctx, "/prombaza undo <id>"))
                .then(CommandManager.argument("id", StringArgumentType.word())
                    .executes(PrombazaCommand::undo))));
    }

    private static BlockPos readCoords(CommandContext<ServerCommandSource> ctx) {
        return new BlockPos(IntegerArgumentType.getInteger(ctx, "x"),
                            IntegerArgumentType.getInteger(ctx, "y"),
                            IntegerArgumentType.getInteger(ctx, "z"));
    }

    private static int setPos(CommandContext<ServerCommandSource> ctx, boolean first, BlockPos given) {
        ServerCommandSource source = ctx.getSource();
        BlockPos pos = given != null ? given : BlockPos.ofFloored(source.getPosition());
        if (first) {
            pendingPos1 = pos;
        } else {
            pendingPos2 = pos;
        }
        // Drop a bright mark on the ground so the player can SEE the corner.
        ServerWorld world = source.getWorld();
        {
            GroundWorks.showCorner(world, pos.getX(), pos.getZ());
        }
        String which = first ? "1" : "2";
        source.sendFeedback(() -> Text.literal(
            "Prombaza corner " + which + " set at " + fmt(pos)
            + " (a torch marks it)."), false);

        if (pendingPos1 != null && pendingPos2 != null) {
            // Show the rectangle the player is choosing, so it is not a blind guess.
            com.ben.csp.enterprise.Footprint plot = com.ben.csp.enterprise.Footprint.of(
                pendingPos1.getX(), pendingPos1.getZ(), pendingPos2.getX(), pendingPos2.getZ());
            GroundWorks.previewOutline(world, plot);
            int w = Math.abs(pendingPos2.getX() - pendingPos1.getX());
            int d = Math.abs(pendingPos2.getZ() - pendingPos1.getZ());
            source.sendFeedback(() -> Text.literal(
                "Both corners set: " + w + " x " + d + " m. Corner 1 at " + fmt(pendingPos1)
                + ", corner 2 at " + fmt(pendingPos2) + "."), false);
            source.sendFeedback(() -> Text.literal(
                "Now give the territory to the enterprises with: "
                + "/directorate designate pb1"), false);
        } else {
            source.sendFeedback(() -> Text.literal(
                "Now walk to the opposite corner and run /prombaza pos2."), false);
        }
        return 1;
    }

    private static int designate(CommandContext<ServerCommandSource> ctx, String name) {
        ServerCommandSource source = ctx.getSource();
        if (pendingPos1 == null || pendingPos2 == null) {
            source.sendError(Text.literal("Set both corners first: /prombaza pos1 then /prombaza pos2"));
            return 0;
        }
        String id = StringArgumentType.getString(ctx, "id");
        Area area = Area.of(pendingPos1.getX(), pendingPos1.getY(), pendingPos1.getZ(),
                            pendingPos2.getX(), pendingPos2.getY(), pendingPos2.getZ());
        // The territory is not ours to take: it goes to the Director as a request.
        String requestId = "prom-" + id;
        try {
            com.ben.csp.directorate.DirectorateState.get(source.getServer())
                .workflow().submit(new com.ben.csp.directorate.DirectorateRequest(
                    requestId, com.ben.csp.directorate.RequestType.PROMBAZA_DESIGNATION,
                    name + " (" + area.describe() + ")", null, null, area,
                    source.getName(), day(source)));
            com.ben.csp.directorate.DirectorateState.get(source.getServer()).markDirty();
        } catch (IllegalStateException e) {
            source.sendError(Text.literal(e.getMessage()));
            return 0;
        }
        source.sendFeedback(() -> Text.literal(
            "Request submitted to the Directorate: " + requestId
            + " — approve it with /directorate approve " + requestId), true);
        return 1;
    }

    private static int list(CommandContext<ServerCommandSource> ctx) {
        PrombazaRegistry reg = PrombazaState.get(ctx.getSource().getServer()).registry();
        if (reg.isEmpty()) {
            ctx.getSource().sendFeedback(() -> Text.literal("No prombaza designated."), false);
            return 1;
        }
        for (Prombaza p : reg.all()) {
            ctx.getSource().sendFeedback(() -> Text.literal(" - " + p), false);
        }
        return reg.size();
    }

    /**
     * Show the входной контроль desk for a prombaza. The desk is the prombaza's
     * own compliance gate: the deliveries it has received, what it did with each,
     * what the checking has cost, and how many installed items are still carrying
     * a defect nobody found.
     */
    private static int control(CommandContext<ServerCommandSource> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        var r = com.ben.csp.zone.ZoneOperations.prombazaReport(ctx.getSource().getServer(), id);
        if (!r.ok()) {
            ctx.getSource().sendError(Text.literal(String.join(" ", r.lines())
                + " Run /prombaza list to see the ids."));
            return 0;
        }
        for (String line : r.lines()) {
            ctx.getSource().sendFeedback(() -> Text.literal(line), false);
        }
        return r.lines().size();
    }

    /**
     * Show the rail siding: what is standing, how long, and the fine it is costing.
     * This is the pressure the входной контроль exists to relieve — a backed-up
     * yard is a fine, and a rushed desk.
     */
    private static int yard(CommandContext<ServerCommandSource> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        if (PrombazaState.get(ctx.getSource().getServer()).registry().byId(id).isEmpty()) {
            ctx.getSource().sendError(Text.literal("No prombaza with id '" + id
                + "'. Run /prombaza list to see the ids."));
            return 0;
        }
        PrombazaState state = PrombazaState.get(ctx.getSource().getServer());
        GoodsYard yard = state.yard(id);
        long today = day(ctx.getSource());
        ctx.getSource().sendFeedback(() -> Text.literal(
            "Siding at prombaza '" + id + "': " + yard.standingCount() + " wagon(s) standing,"
            + " free time " + (int) GoodsYard.FREE_TIME_DAYS + " days."), false);
        if (yard.standingCount() == 0) {
            ctx.getSource().sendFeedback(() -> Text.literal(
                "  The yard is clear — no demurrage. Use /prombaza receive " + id
                + " <n> to work the standing wagons through входной контроль."), false);
            return 1;
        }
        for (String line : yard.standingReport(today)) {
            ctx.getSource().sendFeedback(() -> Text.literal("  " + line), false);
        }
        ctx.getSource().sendFeedback(() -> Text.literal(
            "  demurrage owed so far: " + Math.round(yard.demurrage(today))
            + " — every day it stands, it grows."), false);
        return yard.standingCount();
    }

    /**
     * Work the standing wagons through входной контроль. The check is rushed by the
     * backlog, so clearing a big queue in one go lets more defects through — the
     * exact trade the yard's demurrage creates.
     */
    private static int receive(CommandContext<ServerCommandSource> ctx, String id, int n) {
        var r = com.ben.csp.zone.ZoneOperations.workWagons(
            ctx.getSource().getServer(), id, n, day(ctx.getSource()));
        if (!r.ok()) {
            ctx.getSource().sendFeedback(() -> Text.literal(String.join(" ", r.lines())), false);
            return 0;
        }
        for (String line : r.lines()) {
            ctx.getSource().sendFeedback(() -> Text.literal(line), true);
        }
        return 1;
    }

    private static int undo(CommandContext<ServerCommandSource> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        PrombazaState state = PrombazaState.get(ctx.getSource().getServer());
        var found = state.registry().byId(id);
        if (found.isEmpty()) {
            ctx.getSource().sendError(Text.literal("No prombaza with id '" + id
                + "'. Run /prombaza list to see the ids."));
            return 0;
        }
        // Clear the ground marks first: undo must undo what the player can SEE,
        // not only what is on the record.
        Prombaza p = found.get();
        ServerWorld world = ctx.getSource().getWorld();
        GroundWorks.clearTerritory(world, com.ben.csp.zone.PrombazaTerritory.standard(id, p.area()));
        state.registry().undo(id);
        state.markDirty();
        ctx.getSource().sendFeedback(() -> Text.literal(
            "Undid prombaza '" + id + "' — the boundary marks are cleared from the ground."), true);
        if (com.ben.csp.zone.ZoneState.get(ctx.getSource().getServer()).isSpawned()) {
            ctx.getSource().sendFeedback(() -> Text.literal(
                "Note: the enterprises on the zone are still standing. /zone clear removes "
                + "their ground marks."), false);
        }
        return 1;
    }

    private static int usage(CommandContext<ServerCommandSource> ctx, String text) {
        ctx.getSource().sendError(Text.literal("Usage: " + text + "  (see /csp help)"));
        return 0;
    }

    private static long day(ServerCommandSource source) {
        return source.getWorld().getTimeOfDay() / 24000L + 1L;
    }

    private static String fmt(BlockPos p) {
        return "(" + p.getX() + " " + p.getY() + " " + p.getZ() + ")";
    }
}
