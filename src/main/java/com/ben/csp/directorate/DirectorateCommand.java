package com.ben.csp.directorate;

import com.ben.csp.prombaza.Prombaza;
import com.ben.csp.prombaza.PrombazaCommand;
import com.ben.csp.prombaza.PrombazaState;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/**
 * The Director's powers. This is the admins' tool: the in-tray of matters put to
 * the Director, and the decisions on them. Nothing is designated until the
 * Director approves it.
 *
 *   /directorate list                     show the in-tray
 *   /directorate approve <id>             approve a request
 *   /directorate deny <id> [reason]       deny a request
 *   /directorate designate <id> <name>    designate a prombaza at the corners set
 *                                         with /prombaza pos1 and pos2 (one step)
 */
public final class DirectorateCommand {

    private DirectorateCommand() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> root = CommandManager.literal("directorate")
            .requires(source -> source.hasPermissionLevel(2))
            .then(CommandManager.literal("list").executes(DirectorateCommand::list))
            .then(CommandManager.literal("approve")
                .then(CommandManager.argument("id", StringArgumentType.word())
                    .executes(DirectorateCommand::approve)))
            .then(CommandManager.literal("deny")
                .then(CommandManager.argument("id", StringArgumentType.word())
                    .executes(ctx -> deny(ctx, "denied"))
                    .then(CommandManager.argument("reason", StringArgumentType.greedyString())
                        .executes(ctx -> deny(ctx, StringArgumentType.getString(ctx, "reason"))))))
            .then(CommandManager.literal("designate")
                .then(CommandManager.argument("id", StringArgumentType.word())
                    .executes(ctx -> designate(ctx, StringArgumentType.getString(ctx, "id")))
                    .then(CommandManager.argument("name", StringArgumentType.greedyString())
                        .executes(ctx -> designate(ctx, StringArgumentType.getString(ctx, "name"))))));
        dispatcher.register(root);
    }

    private static int list(CommandContext<ServerCommandSource> ctx) {
        DirectorateState state = DirectorateState.get(ctx.getSource().getServer());
        var pending = state.workflow().pending();
        if (pending.isEmpty()) {
            ctx.getSource().sendFeedback(() -> Text.literal("The in-tray is empty."), false);
            return 1;
        }
        ctx.getSource().sendFeedback(() -> Text.literal(
            "Director's in-tray (" + pending.size() + "):"), false);
        for (DirectorateRequest r : pending) {
            ctx.getSource().sendFeedback(() -> Text.literal(
                "  " + r.id() + " — " + r.type().label() + ": " + r.subject()), false);
        }
        return pending.size();
    }

    private static int approve(CommandContext<ServerCommandSource> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        DirectorateState state = DirectorateState.get(ctx.getSource().getServer());
        DirectorateRequest r;
        try {
            r = state.workflow().approve(id, ctx.getSource().getName(), day(ctx));
        } catch (RuntimeException e) {
            ctx.getSource().sendError(Text.literal(e.getMessage()));
            return 0;
        }
        // An approved prombaza designation is carried out now.
        if (r.type() == RequestType.PROMBAZA_DESIGNATION && r.area() != null) {
            String pbId = id.startsWith("prom-") ? id.substring("prom-".length()) : id;
            String name = r.subject().contains(" (")
                ? r.subject().substring(0, r.subject().indexOf(" (")) : r.subject();
            PrombazaState pbState = PrombazaState.get(ctx.getSource().getServer());
            try {
                Prombaza p = pbState.registry().designate(pbId, name, r.area(), day(ctx),
                    ctx.getSource().getName());
                pbState.markDirty();
                ctx.getSource().sendFeedback(() -> Text.literal("APPROVED and designated: " + p), true);
            } catch (IllegalStateException e) {
                ctx.getSource().sendError(Text.literal("approved, but could not designate: " + e.getMessage()));
                return 0;
            }
        } else {
            ctx.getSource().sendFeedback(() -> Text.literal("APPROVED: " + r.id()), true);
        }
        state.markDirty();
        return 1;
    }

    private static int deny(CommandContext<ServerCommandSource> ctx, String reason) {
        String id = StringArgumentType.getString(ctx, "id");
        DirectorateState state = DirectorateState.get(ctx.getSource().getServer());
        try {
            state.workflow().deny(id, ctx.getSource().getName(), day(ctx), reason);
        } catch (RuntimeException e) {
            ctx.getSource().sendError(Text.literal(e.getMessage()));
            return 0;
        }
        state.markDirty();
        ctx.getSource().sendFeedback(() -> Text.literal("DENIED: " + id + " — " + reason), true);
        return 1;
    }

    private static int designate(CommandContext<ServerCommandSource> ctx, String name) {
        BlockPos p1 = PrombazaCommand.pendingPos1();
        BlockPos p2 = PrombazaCommand.pendingPos2();
        if (p1 == null || p2 == null) {
            ctx.getSource().sendError(Text.literal(
                "Set the territory corners first: /prombaza pos1, then /prombaza pos2."));
            return 0;
        }
        String id = StringArgumentType.getString(ctx, "id");
        com.ben.csp.prombaza.Area area = com.ben.csp.prombaza.Area.of(
            p1.getX(), p1.getY(), p1.getZ(), p2.getX(), p2.getY(), p2.getZ());
        PrombazaState pbState = PrombazaState.get(ctx.getSource().getServer());
        try {
            Prombaza p = pbState.registry().designate(id, name, area, day(ctx),
                ctx.getSource().getName());
            pbState.markDirty();
            PrombazaCommand.clearPendingCorners();
            ctx.getSource().sendFeedback(() -> Text.literal(
                "The Director designates: " + p), true);
            return 1;
        } catch (IllegalStateException e) {
            ctx.getSource().sendError(Text.literal(e.getMessage()));
            return 0;
        }
    }

    private static long day(CommandContext<ServerCommandSource> ctx) {
        return ctx.getSource().getWorld().getTimeOfDay() / 24000L + 1L;
    }
}
