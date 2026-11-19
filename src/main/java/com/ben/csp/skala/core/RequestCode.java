package com.ben.csp.skala.core;

import com.ben.csp.rbmk.plant.Instrumentation;
import com.ben.csp.rbmk.plant.PlantSystem;

import java.util.List;

/**
 * A SKALA request code — the way an operator addressed the computer.
 *
 * <p>Reconstructed from the Chornobyl Family SKALA restoration (the operator
 * console episodes): the code is entered on a request device as <b>seven
 * columns</b> and answers the question "which parameter of which piece of
 * equipment do I want?":
 *
 * <pre>
 *   col 1 : equipment GROUP      a Cyrillic letter (К=channel, А=reactor,
 *                                Н=pumps, Д=deaerator, ...)
 *   col 2 : SECTION of that group a digit
 *   col 3 : PARAMETER TYPE       a Latin letter (N=power, H=level,
 *                                P=pressure, G=flow/consumption, ...)
 *   col 4-7: INSTANCE            four digits — a device number, or for a
 *                                reactor channel the two-octal-digit
 *                                horizontal/vertical coordinates
 * </pre>
 *
 * <p>Example codes from the source: {@code А0Р3214} = reactor, pressure,
 * device 3214 (answer "6 kgf/cm2"); {@code К0G4136} = channel, flow, channel at
 * (41,36) — the coolant flow through that channel. These resolve to a real
 * {@link Instrumentation} signal, so the number the operator reads is the number
 * the physics holds.
 *
 * <p>The vocabulary of codes is deliberately large — an operator had to know
 * thousands and recall one instantly — so resolution is table-driven, not a
 * switch.
 */
public final class RequestCode {

    /** The first column: the equipment group. Unknown letters are kept, flagged. */
    public enum Group {
        K('К', "канал", "reactor channel", true),
        U('У', "?", "unknown group (transcript lists it; meaning unresolved)", false),
        A('А', "реактор", "reactor", true),
        P('П', "?", "unknown group (transcript lists it; meaning unresolved)", false),
        N('Н', "насосы", "pumps", true),
        D('Д', "деаэратор", "deaerator", true),
        T('Т', "турбина", "turbine (probable; flagged)", false),
        M('М', "?", "unknown group (transcript lists it; meaning unresolved)", false);

        public final char letter;
        public final String ru;
        public final String en;
        public final boolean known;

        Group(char letter, String ru, String en, boolean known) {
            this.letter = letter; this.ru = ru; this.en = en; this.known = known;
        }

        public static Group of(char c) {
            for (Group g : values()) {
                if (g.letter == c) return g;
            }
            return null;
        }
    }

    /** The third column: the parameter type. */
    public enum Param {
        X('X', "?", "unknown parameter", false),
        N('N', "мощность", "power", true),
        S('S', "запас", "reserve (reactivity margin)", true),
        H('H', "уровень", "level", true),
        P('P', "давление", "pressure", true),
        G('G', "расход", "flow (consumption)", true),
        T('T', "температура", "temperature (probable; flagged)", false);

        public final char letter;
        public final String ru;
        public final String en;
        public final boolean known;

        Param(char letter, String ru, String en, boolean known) {
            this.letter = letter; this.ru = ru; this.en = en; this.known = known;
        }

        public static Param of(char c) {
            for (Param p : values()) {
                if (p.letter == c) return p;
            }
            return null;
        }
    }

    /** The answer to a request: the value, its unit, and whether it deviates. */
    public record Answer(boolean found, String echo, double value, String unit,
                         String description, boolean deviates, String note) {
        public static Answer notFound(String echo, String why) {
            return new Answer(false, echo, Double.NaN, "", why, false, why);
        }
    }

    private final Group group;
    private final int section;
    private final Param param;
    private final int digits;

    private RequestCode(Group group, int section, Param param, int digits) {
        this.group = group; this.section = section;
        this.param = param; this.digits = digits;
    }

    /**
     * Parse a code like {@code К0G4136} or {@code А0Р3214}. Returns null if the
     * string is not a well-formed seven-column code.
     */
    public static RequestCode parse(String s) {
        if (s == null) return null;
        String t = s.trim().toUpperCase().replace(" ", "");
        if (t.length() != 7) return null;
        Group g = Group.of(t.charAt(0));
        if (g == null) return null;
        if (!Character.isDigit(t.charAt(1))) return null;
        Param p = Param.of(t.charAt(2));
        if (p == null) return null;
        for (int i = 3; i < 7; i++) {
            if (!Character.isDigit(t.charAt(i))) return null;
        }
        return new RequestCode(g, t.charAt(1) - '0', p, Integer.parseInt(t.substring(3)));
    }

    public Group group() { return group; }
    public Param param() { return param; }
    public int section() { return section; }
    public int digits() { return digits; }

    @Override
    public String toString() {
        return String.format("%c%d%c%04d", group.letter, section, param.letter, digits);
    }

    /** A human description of what the code asks for. */
    public String describe() {
        return String.format("%s %s, section %d, %s, instance %04d",
            group.letter, group.en, section, param.en, digits);
    }

    /**
     * Resolve this code against the live plant and the instrumentation register.
     * This is where the request device stops being decorative: the value returned
     * is read from the running {@link PlantSystem}.
     */
    public Answer resolve(PlantSystem plant, List<Instrumentation.Signal> signals) {
        String echo = toString();
        if (plant == null) {
            return Answer.notFound(echo, "plant not running");
        }

        // Reactor-wide parameters (group А).
        if (group == Group.A) {
            return switch (param) {
                case N -> new Answer(true, echo, plant.core().power() * 100.0, "% Nnom",
                    "Reactor thermal power", false, "");
                // К0S0000 is the ORM code the operators used, and SKALA grouped it
                // under К, but it is a reactor-wide margin, so accept it on А too.
                case S -> ormAnswer(plant, echo);
                case P -> {
                    double bar = plant.separators().length > 0
                        ? plant.separators()[0].pressurePa() / 1e5 : Double.NaN;
                    yield new Answer(true, echo, bar, "bar",
                        "Drum-separator pressure", false, "");
                }
                case G -> {
                    double m3s = 0;
                    for (var p : plant.pumps()) m3s += p.flowM3s();
                    yield new Answer(true, echo, m3s * 3600.0, "m3/h",
                        "Total MCP flow", false, "");
                }
                default -> Answer.notFound(echo, "unsupported reactor parameter " + param.letter);
            };
        }

        // The operating reactivity margin. Its SKALA code is К0S0000 — group К,
        // section 0, parameter S, instance 0000 — because the margin is a property
        // of the reactor as a whole rather than of any one channel.
        if (param == Param.S && section == 0 && digits == 0) {
            return ormAnswer(plant, echo);
        }

        // Per-channel parameters (group К). The four digits are the channel's
        // two-octal-digit coordinates; the exact display convention is a known
        // unknown, so both a decimal and an octal reading are tried.
        if (group == Group.K) {
            int[] coord = channelCoord(digits);
            if (coord == null) {
                return Answer.notFound(echo, "no such channel " + String.format("%04d", digits));
            }
            Instrumentation.Kind kind = switch (param) {
                case G -> Instrumentation.Kind.CHANNEL_FLOW;
                case T -> Instrumentation.Kind.CHANNEL_TEMP;
                case H -> Instrumentation.Kind.CHANNEL_VOID; // void/level in the channel
                default -> null;
            };
            if (kind == null) {
                return Answer.notFound(echo, "unsupported channel parameter " + param.letter);
            }
            for (Instrumentation.Signal s : signals) {
                if (s.kind() == kind && s.index() == coord[0]) {
                    double v = Instrumentation.read(plant, s);
                    boolean dev = kind == Instrumentation.Kind.CHANNEL_FLOW
                        && v < 4.0; // well below nominal channel flow
                    return new Answer(true, echo, v, s.unit(),
                        s.nameEn() + " " + s.code(), dev, "");
                }
            }
            return Answer.notFound(echo, "channel " + coord[0] + " not in register");
        }

        // Pumps (group Н).
        if (group == Group.N) {
            Instrumentation.Kind kind = param == Param.G ? Instrumentation.Kind.PUMP_FLOW
                : param == Param.N ? Instrumentation.Kind.PUMP_SPEED : null;
            if (kind == null) return Answer.notFound(echo, "unsupported pump parameter");
            for (Instrumentation.Signal s : signals) {
                if (s.kind() == kind && s.index() == (digits - 1)) {
                    return new Answer(true, echo, Instrumentation.read(plant, s), s.unit(),
                        s.nameEn() + " " + s.code(), false, "");
                }
            }
            return Answer.notFound(echo, "no pump " + digits);
        }

        // Deaerator (group Д) — level/pressure family; index by section.
        if (group == Group.D) {
            Instrumentation.Kind kind = param == Param.H ? Instrumentation.Kind.DRUM_LEVEL
                : param == Param.P ? Instrumentation.Kind.DRUM_PRESSURE : null;
            if (kind == null) return Answer.notFound(echo, "unsupported deaerator parameter");
            for (Instrumentation.Signal s : signals) {
                if (s.kind() == kind && s.index() == (section - 1)) {
                    return new Answer(true, echo, Instrumentation.read(plant, s), s.unit(),
                        s.nameEn() + " " + s.code(), false, "");
                }
            }
            return Answer.notFound(echo, "no deaerator " + section);
        }

        return Answer.notFound(echo,
            "group " + group.letter + " (" + group.en + ") has no live source yet");
    }

    /**
     * The operating reactivity margin as SKALA answers it: a count of equivalent
     * fully inserted manual rods, flagged against the plant's set points.
     */
    private static Answer ormAnswer(PlantSystem plant, String echo) {
        if (!(plant.core() instanceof com.ben.csp.rbmk.physics.UnifiedReactorCore u)) {
            return Answer.notFound(echo, "ORM needs the unified core");
        }
        double orm = u.orm();
        boolean deviates = orm < 43.0;
        String note = orm < 30.0 ? "BELOW 30 rods: operation not permitted"
            : orm < 43.0 ? "below the 43-rod warning set point" : "";
        return new Answer(true, echo, orm, "rods",
            "Operating reactivity margin", deviates, note);
    }

    /**
     * Map the four instance digits to a channel index. Tries the digits as two
     * two-digit octal coordinates first (the documented display form), then as a
     * plain index.
     */
    private static int[] channelCoord(int digits) {
        int hi = digits / 100;
        int lo = digits % 100;
        // documented form: two octal two-digit numbers (horizontal, vertical)
        int ox = Integer.parseInt(String.format("%02d", hi), 8);
        int oz = Integer.parseInt(String.format("%02d", lo), 8);
        int idx = Instrumentation.channelIndexByCoord(ox, oz);
        if (idx >= 0) return new int[]{idx, ox, oz};
        idx = Instrumentation.channelIndexByCoord(hi, lo);
        if (idx >= 0) return new int[]{idx, hi, lo};
        return null;
    }
}
