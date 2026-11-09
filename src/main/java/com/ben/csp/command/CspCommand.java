package com.ben.csp.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * The mod's command list. Typing a command wrong, or not knowing one exists, is a
 * dead end; this lists what there is and what each does, in plain words.
 */
public final class CspCommand {

    private CspCommand() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("csp")
            .executes(CspCommand::help)
            .then(CommandManager.literal("help").executes(CspCommand::help)));
    }

    private static int help(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource s = ctx.getSource();
        header(s, "CSP-Modern — commands");
        line(s, "/csp help", "this list");
        blank(s);
        header(s, "Set up the prombaza (the production base)");
        line(s, "/prombaza pos1", "mark one corner of the territory where you stand");
        line(s, "/prombaza pos2", "mark the opposite corner");
        line(s, "/prombaza list", "list the designated territories and their ids");
        line(s, "/prombaza designate <id> [name]", "submit a territory request to the Director");
        line(s, "/prombaza undo <id>", "remove a territory and clear its ground marks");
        blank(s);
        header(s, "The Director's powers");
        line(s, "/directorate list", "the Director's in-tray");
        line(s, "/directorate approve <id>", "approve a request (a designation is carried out)");
        line(s, "/directorate deny <id> [reason]", "deny a request");
        line(s, "/directorate designate <id> [name]", "designate a territory at your two corners");
        blank(s);
        header(s, "The zone (the enterprises)");
        line(s, "/zone spawn [radius]", "put the ten enterprises on the territory");
        line(s, "/zone status", "how far each enterprise has come");
        line(s, "/zone draw", "redraw the territory and the facilities on the ground");
        line(s, "/zone clear", "remove the ground marks (the zone itself stays)");
        blank(s);
        s.sendFeedback(() -> Text.literal("Typical first run: /prombaza pos1, /prombaza pos2, "
            + "/directorate designate pb1, /zone spawn, then /zone status.").formatted(Formatting.GRAY), false);
        return 1;
    }

    private static void header(ServerCommandSource s, String text) {
        s.sendFeedback(() -> Text.literal(text).formatted(Formatting.GOLD), false);
    }

    private static void line(ServerCommandSource s, String cmd, String what) {
        s.sendFeedback(() -> Text.literal("  " + cmd + "  —  " + what)
            .formatted(Formatting.WHITE), false);
    }

    private static void blank(ServerCommandSource s) {
        s.sendFeedback(() -> Text.literal(" "), false);
    }
}
