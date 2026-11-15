package com.ben.csp.skala;

import com.ben.csp.rbmk.plant.PlantSystem;
import com.ben.csp.rbmk.plant.PlantTicker;
import com.ben.csp.skala.core.DivtConsole;
import com.ben.csp.skala.core.DregRecorder;
import com.ben.csp.skala.core.DrumPrinter;
import com.ben.csp.skala.core.FunctionalGroup;
import com.ben.csp.skala.core.RequestCode;
import com.ben.csp.skala.core.SkalaComplex;
import com.ben.csp.skala.core.SkalaProgram;
import com.ben.csp.skala.core.SkalaRole;
import com.ben.csp.skala.core.SkalaTapeDrive;
import com.ben.csp.skala.core.SkalaTicker;
import com.ben.csp.skala.core.Teletype;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * {@code /skala ...} — drive and inspect the SKALA process computer in game.
 *
 * <p>The computer runs on the server clock and reads the live plant's
 * instrumentation register, so every number it reports is a real one from the
 * running physics. Commands mirror what an operator or the DIVT would do:
 * start the operational set, enter a request code, look at the status display,
 * the tape inventory and the program list.
 */
public final class SkalaCommand {

    private SkalaCommand() {}

    public static void register(CommandDispatcher<ServerCommandSource> d) {
        d.register(CommandManager.literal("skala")
            .then(CommandManager.literal("start").executes(SkalaCommand::start))
            .then(CommandManager.literal("halt").executes(SkalaCommand::halt))
            .then(CommandManager.literal("status").executes(SkalaCommand::status))
            .then(CommandManager.literal("programs").executes(SkalaCommand::programs))
            .then(CommandManager.literal("drives").executes(SkalaCommand::drives))
            .then(CommandManager.literal("groups").executes(SkalaCommand::groups))
            .then(CommandManager.literal("tape").executes(SkalaCommand::tape))
            .then(CommandManager.literal("teletype")
                .executes(ctx -> teletype(ctx, "control"))
                .then(CommandManager.argument("job", StringArgumentType.word())
                    .executes(ctx -> teletype(ctx, StringArgumentType.getString(ctx, "job")))))
            .then(CommandManager.literal("print")
                .executes(ctx -> print(ctx, "cartogram"))
                .then(CommandManager.argument("what", StringArgumentType.word())
                    .executes(ctx -> print(ctx, StringArgumentType.getString(ctx, "what")))))
            .then(CommandManager.literal("divt").executes(SkalaCommand::divt))
            .then(CommandManager.literal("roles").executes(SkalaCommand::roles))
            .then(CommandManager.literal("alarm")
                .executes(ctx -> alarm(ctx, true))
                .then(CommandManager.literal("off").executes(ctx -> alarm(ctx, false))))
            .then(CommandManager.literal("request")
                .then(CommandManager.argument("code", StringArgumentType.word())
                    .executes(SkalaCommand::request))));
    }

    private static int start(CommandContext<ServerCommandSource> ctx) {
        SkalaTicker.start();
        feedback(ctx.getSource(), "SKALA: operational set started (configuration 2.0). "
            + "DREG registration loop running; PRIZMA loaded from drive 2.", Formatting.GREEN);
        return 1;
    }

    private static int halt(CommandContext<ServerCommandSource> ctx) {
        SkalaTicker.halt();
        feedback(ctx.getSource(), "SKALA: halted. Processors stopped.", Formatting.YELLOW);
        return 1;
    }

    private static int status(CommandContext<ServerCommandSource> ctx) {
        SkalaComplex c = SkalaTicker.complex();
        PlantSystem plant = PlantTicker.plant();
        feedback(ctx.getSource(), String.format(
            "SKALA status: %s  processors A=%s B=%s  config=%s%n"
            + "  signals registered: %d   scanned: %d   DREG tape: %d entries (%d with processing lag)%n"
            + "  deviations seen: %d   alarm: %s",
            c.running() ? "RUNNING" : "IDLE",
            c.status().stateA(), c.status().stateB(), c.status().configuration(),
            c.signals().size(), c.signalsScanned(), c.dreg().size(), c.dreg().laggedEntries(),
            c.deviationsSeen(), c.status().alarmSounding() ? "SOUNDING" : "silent"),
            Formatting.AQUA);
        if (plant == null) {
            feedback(ctx.getSource(), "  (plant not running — SKALA has no live inputs)",
                Formatting.GRAY);
        }
        return 1;
    }

    private static int programs(CommandContext<ServerCommandSource> ctx) {
        StringBuilder sb = new StringBuilder("SKALA main programs (status-display panel 3):\n");
        for (SkalaProgram p : SkalaProgram.values()) {
            sb.append(String.format("  %-7s %-7s %s%s%n",
                p.ru, p.latin, p.description,
                p.known ? "" : "  [name known, description unknown]"));
        }
        feedback(ctx.getSource(), sb.toString().stripTrailing(), Formatting.AQUA);
        return 1;
    }

    private static int drives(CommandContext<ServerCommandSource> ctx) {
        SkalaComplex c = SkalaTicker.complex();
        StringBuilder sb = new StringBuilder("SKALA tape pool (six ES-5070 reel drives):\n");
        for (String line : c.tapeInventory()) {
            sb.append("  ").append(line).append('\n');
        }
        feedback(ctx.getSource(), sb.toString().stripTrailing(), Formatting.AQUA);
        return 1;
    }

    private static int groups(CommandContext<ServerCommandSource> ctx) {
        SkalaComplex c = SkalaTicker.complex();
        StringBuilder sb = new StringBuilder("SKALA functional control groups (24):\n");
        for (FunctionalGroup g : c.status().groups()) {
            sb.append(String.format("  %2d  %-10s %s%n", g.number, g.state(), g.job));
        }
        feedback(ctx.getSource(), sb.toString().stripTrailing(), Formatting.AQUA);
        return 1;
    }

    private static int tape(CommandContext<ServerCommandSource> ctx) {
        SkalaComplex c = SkalaTicker.complex();
        var t = c.dreg().tape();
        int n = Math.min(15, t.size());
        StringBuilder sb = new StringBuilder("DREG tape — last " + n + " of " + t.size()
            + " entries (note: stamped at processing time, not measurement):\n");
        for (int i = t.size() - n; i < t.size(); i++) {
            DregRecorder.Entry e = t.get(i);
            sb.append(String.format("  %8.1fs  %-14s %-18s %9.3f %-6s %s%n",
                e.processedAtS(), e.code(), e.type(), e.value(), e.unit(), e.note()));
        }
        feedback(ctx.getSource(), sb.toString().stripTrailing(), Formatting.WHITE);
        return 1;
    }

    private static int teletype(CommandContext<ServerCommandSource> ctx, String job) {
        SkalaComplex c = SkalaTicker.complex();
        Teletype.Job j = switch (job.toLowerCase()) {
            case "operator", "actions" -> Teletype.Job.OPERATOR_ACTIONS;
            case "malfunctions", "faults" -> Teletype.Job.MALFUNCTIONS;
            case "control", "controlroom" -> Teletype.Job.CONTROL_ROOM;
            case "reserve" -> Teletype.Job.RESERVE;
            default -> Teletype.Job.SETPOINTS;
        };
        Teletype tt = c.teletype(j);
        StringBuilder sb = new StringBuilder("RDA-80 teletype [" + j + "] — "
            + tt.paper().size() + " lines printed, " + tt.queued() + " queued (10 s/line):\n");
        var paper = tt.paper();
        int n = Math.min(12, paper.size());
        for (int i = paper.size() - n; i < paper.size(); i++) {
            sb.append("  ").append(paper.get(i).text()).append('\n');
        }
        if (paper.isEmpty()) {
            sb.append("  (nothing printed yet — the head is still on its first line)\n");
        }
        feedback(ctx.getSource(), sb.toString().stripTrailing(), Formatting.WHITE);
        return 1;
    }

    private static int print(CommandContext<ServerCommandSource> ctx, String what) {
        SkalaComplex c = SkalaTicker.complex();
        PlantSystem plant = PlantTicker.plant();
        java.util.List<String> lines = switch (what.toLowerCase()) {
            case "rods", "rodmap" -> c.drumPrinter(DrumPrinter.Unit.PARAMETERS).rodMap(c.signals());
            case "deviations" -> c.drumPrinter(DrumPrinter.Unit.DEVIATIONS).cartogram(plant, c.signals());
            default -> c.drumPrinter(DrumPrinter.Unit.PARAMETERS).cartogram(plant, c.signals());
        };
        feedback(ctx.getSource(), String.join("\n", lines), Formatting.WHITE);
        return 1;
    }

    private static int divt(CommandContext<ServerCommandSource> ctx) {
        SkalaComplex c = SkalaTicker.complex();
        StringBuilder sb = new StringBuilder("DIVT console (chief computer engineer, computer room):\n");
        sb.append("  library tape mounted: ").append(c.divt().tapeMounted())
          .append(" (").append(c.divt().mountedTape()).append(")\n");
        for (DivtConsole.PunchDevice d : DivtConsole.PunchDevice.values()) {
            sb.append("  ").append(d.model).append(" — ").append(d.description).append('\n');
        }
        feedback(ctx.getSource(), sb.toString().stripTrailing(), Formatting.AQUA);
        return 1;
    }

    private static int roles(CommandContext<ServerCommandSource> ctx) {
        StringBuilder sb = new StringBuilder(
            "Operating personnel (Soviet abbreviations, per the 1Э-С-II-1983 unit 3/4 regulation):\n");
        for (SkalaRole r : SkalaRole.values()) {
            sb.append(String.format("  %-6s %-45s %-18s %s%n",
                r.abbr, r.ru, r.workplace, r.en));
        }
        sb.append("  Control-room posts (each has a request device): ");
        for (SkalaRole.ControlRoomPost p : SkalaRole.ControlRoomPost.values()) {
            sb.append(p.abbr).append(' ');
        }
        sb.append("\n  Computer-room role: DIVT (chief/senior engineer, computer engineering)");
        feedback(ctx.getSource(), sb.toString().stripTrailing(), Formatting.AQUA);
        return 1;
    }

    private static int alarm(CommandContext<ServerCommandSource> ctx, boolean on) {
        SkalaComplex c = SkalaTicker.complex();
        if (on) {
            c.status().raiseMalfunction();
            feedback(ctx.getSource(), "SKALA: malfunction — alarm sounding, indication blinking.",
                Formatting.RED);
        } else {
            c.status().silenceAlarm();
            c.status().acknowledge();
            feedback(ctx.getSource(), "SKALA: alarm silenced and acknowledged; "
                + "indication stays lit until the fault clears.", Formatting.YELLOW);
        }
        return 1;
    }

    private static int request(CommandContext<ServerCommandSource> ctx) {
        String code = StringArgumentType.getString(ctx, "code");
        SkalaComplex c = SkalaTicker.complex();
        PlantSystem plant = PlantTicker.plant();
        RequestCode.Answer a = c.request(code, plant);
        if (!a.found()) {
            feedback(ctx.getSource(), "SKALA: " + a.echo() + " — " + a.note(), Formatting.RED);
            return 0;
        }
        String line = String.format("SKALA: %s = %.3f %s   (%s)%s",
            a.echo(), a.value(), a.unit(), a.description(),
            a.deviates() ? "  [DEVIATION]" : "");
        feedback(ctx.getSource(), line, a.deviates() ? Formatting.RED : Formatting.GREEN);
        return 1;
    }

    private static void feedback(ServerCommandSource s, String text, Formatting f) {
        s.sendFeedback(() -> Text.literal(text).formatted(f), false);
    }
}
