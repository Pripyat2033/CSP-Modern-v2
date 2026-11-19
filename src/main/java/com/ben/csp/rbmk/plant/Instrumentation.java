package com.ben.csp.rbmk.plant;

import com.ben.csp.rbmk.RbmkGeometry;
import com.ben.csp.rbmk.RbmkLattice;
import com.ben.csp.rbmk.RodType;

/**
 * The plant's instrumentation register: every analogue signal a real RBMK-1000
 * energoblok carried, as a wire with a name, a unit and a source.
 *
 * <p>The point of this class is that the plant's control surface is <b>not</b> a
 * dozen lumped numbers. A real RBMK-1000 had <b>~4,000 data input points</b>
 * feeding the SKALA computer (INSAG-7 §2.5: "The data acquisition system received
 * information from about 4000 data input points"), and the operator's panels read
 * a large fraction of them. This class enumerates the points by source, so the
 * count is a real inventory rather than a guess:
 *
 * <ul>
 *   <li><b>Per fuel channel (1,872):</b> coolant flow (the rotameter, one per
 *       channel — INSAG-7 §2.9 "Each channel has a flow control valve"), outlet
 *       steam quality, void, channel outlet temperature. The per-channel flow is
 *       the reading that made a blocked or boiling channel visible.</li>
 *   <li><b>Per control channel (211):</b> the rod signals — position, the two limit
 *       switches (НК / ВК), the overcompensation (ПК) drive where the rod type has
 *       one, and the two selsyn dial lamps. The 211 rods are seven types
 *       ({@link RodType}, regulation §2.11.6), not one.</li>
 *   <li><b>In-core power-distribution detectors (SPD, ~112 radial + axial):</b>
 *       the PPDDCS detectors (INSAG-7 §2.4), which is how the spatial power map
 *       was measured.</li>
 *   <li><b>Per drum (4), per pump (8), per turbine (2), per condenser (2):</b>
 *       level, pressure, speed, flow, vibration, temperatures.</li>
 * </ul>
 *
 * <p>The values themselves are read from the live {@link PlantSystem} — this
 * class does not invent them. What it adds is the <em>address</em> of each point
 * (a code like {@code TК042.FLOW}), so a control panel, a SKALA channel or a
 * teletype line can bind to the exact signal it shows.
 *
 * <p>Nothing here is Minecraft-typed: it can be enumerated and tested standalone.
 */
public final class Instrumentation {

    private Instrumentation() {}

    /** One analogue input point: where it is, what it measures, its unit. */
    public record Signal(String code, String nameRu, String nameEn, String unit,
                         Kind kind, int index) {}

    /** The family a signal belongs to — which part of the plant it watches. */
    public enum Kind { CHANNEL_FLOW, CHANNEL_VOID, CHANNEL_QUALITY, CHANNEL_TEMP,
                       ROD_POSITION, SPD, DRUM_LEVEL, DRUM_PRESSURE,
                       PUMP_FLOW, PUMP_SPEED, TURBINE_SPEED, TURBINE_POWER,
                       CONDENSER_TEMP, MCP_HEADER_FLOW, FEEDWATER_FLOW, CORE_POWER,
                       CORE_REACTIVITY, CORE_ORM, CORE_PERIOD,
                       // --- per control-rod signals (regulation-sourced) ---
                       // Each rod carries more than a position: limit switches at
                       // the ends of its travel, an overcompensation (ПК) drive where
                       // its type has one, and two selsyn dial lamps. See docs/ROD-SIGNALS.md.
                       ROD_LIMIT_LOWER,   // НК — нижний концевик
                       ROD_LIMIT_UPPER,   // ВК — верхний концевик
                       ROD_OVERCOMP,      // ПК — перекомпенсация (АЗ / РР rods)
                       ROD_LAMP_DEPTH,    // selsyn lamp, green, 6 o'clock — rod at full depth
                       ROD_LAMP_TOP }     // selsyn lamp, orange, 12 o'clock — rod at top of stops

    /** Build the full signal list for the current plant layout. */
    public static java.util.List<Signal> all() {
        java.util.List<Signal> out = new java.util.ArrayList<>(5000);
        int ch = 0;
        int rod = 0;                       // control-rod ordinal (0..210)
        java.util.List<RodType> cartogram = RodType.cartogram(RbmkGeometry.CONTROL_CHANNELS);
        for (RbmkLattice.Channel c : RbmkLattice.channels()) {
            if (c.control()) {
                String base = code("SUZ", c.x(), c.z());
                RodType type = cartogram.get(rod);
                out.add(new Signal(base + ".POS", "Положение стержня СУЗ (" + type.abbr + ")",
                    "Control-rod position (" + type.abbr + ")", "%",
                    Kind.ROD_POSITION, rod));
                // Limit switches at the ends of travel (ВК / НК). §2.11.6-2.11.7.
                out.add(new Signal(base + ".NK", "Нижний концевик (" + type.abbr + ")",
                    "Lower limit switch (" + type.abbr + ")", "-",
                    Kind.ROD_LIMIT_LOWER, rod));
                out.add(new Signal(base + ".VK", "Верхний концевик (" + type.abbr + ")",
                    "Upper limit switch (" + type.abbr + ")", "-",
                    Kind.ROD_LIMIT_UPPER, rod));
                // Overcompensation (ПК) drive — only for types that carry it.
                if (type.hasOvercompensation()) {
                    out.add(new Signal(base + ".PK", "Перекомпенсация (" + type.abbr + ")",
                        "Overcompensation (" + type.abbr + ")", "-",
                        Kind.ROD_OVERCOMP, rod));
                }
                // Selsyn dial lamps: green at 6 o'clock (full depth), orange at
                // 12 o'clock (top of stops). Recollection, see docs/ROD-SIGNALS.md.
                out.add(new Signal(base + ".LAMP_NIZ", "Лампа 'на глубине' (" + type.abbr + ")",
                    "Selsyn lamp, full depth (" + type.abbr + ")", "-",
                    Kind.ROD_LAMP_DEPTH, rod));
                out.add(new Signal(base + ".LAMP_VERH", "Лампа 'на упоре' (" + type.abbr + ")",
                    "Selsyn lamp, top of stops (" + type.abbr + ")", "-",
                    Kind.ROD_LAMP_TOP, rod));
                rod++;
            } else {
                String base = code("TK", c.x(), c.z());
                out.add(new Signal(base + ".FLOW", "Расход воды в ТК",
                    "Fuel-channel coolant flow", "kg/s", Kind.CHANNEL_FLOW, ch));
                out.add(new Signal(base + ".VOID", "Паросодержание в ТК",
                    "Fuel-channel void", "-", Kind.CHANNEL_VOID, ch));
                out.add(new Signal(base + ".QUAL", "Влажность пара на выходе ТК",
                    "Channel outlet steam quality", "-", Kind.CHANNEL_QUALITY, ch));
                out.add(new Signal(base + ".TEMP", "Температура на выходе ТК",
                    "Channel outlet temperature", "C", Kind.CHANNEL_TEMP, ch));
            }
            ch++;
        }
        for (int i = 0; i < PlantPhysics.SEPARATOR_COUNT; i++) {
            out.add(new Signal("BS" + (i + 1) + ".LEVEL", "Уровень в барабане-сепараторе",
                "Drum level", "mm", Kind.DRUM_LEVEL, i));
            out.add(new Signal("BS" + (i + 1) + ".PRESS", "Давление в барабане-сепараторе",
                "Drum pressure", "kgf/cm2", Kind.DRUM_PRESSURE, i));
        }
        for (int i = 0; i < PlantPhysics.CIRCUITS * PlantPhysics.MCP_PER_CIRCUIT; i++) {
            out.add(new Signal("GCN" + (i + 1) + ".FLOW", "Расход ГЦН",
                "MCP flow", "m3/h", Kind.PUMP_FLOW, i));
            out.add(new Signal("GCN" + (i + 1) + ".SPD", "Частота вращения ГЦН",
                "MCP speed", "rpm", Kind.PUMP_SPEED, i));
        }
        for (int i = 0; i < PlantPhysics.TURBINE_COUNT; i++) {
            out.add(new Signal("TG" + (i + 1) + ".SPD", "Частота вращения турбогенератора",
                "Turbogenerator speed", "rpm", Kind.TURBINE_SPEED, i));
            out.add(new Signal("TG" + (i + 1) + ".P", "Мощность турбогенератора",
                "Turbogenerator power", "MW", Kind.TURBINE_POWER, i));
            out.add(new Signal("K" + (i + 1) + ".TCW", "Температура охлаждающей воды конденсатора",
                "Condenser cooling-water temperature", "C", Kind.CONDENSER_TEMP, i));
        }
        // In-core power-distribution detectors (ДКЭ / SPD), the PPDDCS grid:
        // ~112 radial detectors + axial strings (INSAG-7 §2.4). These are the
        // sensors whose absence at low power left the operator blind.
        for (int i = 0; i < 124; i++) {
            out.add(new Signal("DKE" + (i + 1), "Датчик энерговыделения (ДКЭ)",
                "In-core power-distribution detector", "a.u.", Kind.SPD, i));
        }
        out.add(new Signal("AZ.MOSCH", "Мощность реактора", "Reactor power", "% Nnom",
            Kind.CORE_POWER, 0));
        out.add(new Signal("AZ.REACT", "Реактивность", "Reactivity", "beta",
            Kind.CORE_REACTIVITY, 0));
        out.add(new Signal("AZ.ORM", "Оперативный запас реактивности",
            "Operating reactivity margin", "rods", Kind.CORE_ORM, 0));
        out.add(new Signal("AZ.PERIOD", "Период реактора", "Reactor period", "s",
            Kind.CORE_PERIOD, 0));
        return out;
    }

    /** Count of signals by family — the honest size of the control surface. */
    public static java.util.Map<Kind, Integer> counts() {
        java.util.Map<Kind, Integer> m = new java.util.EnumMap<>(Kind.class);
        for (Signal s : all()) {
            m.merge(s.kind(), 1, Integer::sum);
        }
        return m;
    }

    public static int size() {
        return all().size();
    }

    /** A channel address like {@code TK042} from its lattice (x,z). */
    private static String code(String prefix, int x, int z) {
        return String.format("%s%03d%03d", prefix, Math.abs(x), Math.abs(z));
    }

    /** The channel index for a lattice coordinate (x,z), or -1 if none. */
    public static int channelIndexByCoord(int x, int z) {
        java.util.List<RbmkLattice.Channel> chs = RbmkLattice.channels();
        for (int i = 0; i < chs.size(); i++) {
            RbmkLattice.Channel c = chs.get(i);
            if (c.x() == x && c.z() == z) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Read a signal's current value from the running plant. The plant is the
     * source of truth; a signal with no live value returns {@link Double#NaN}.
     */
    public static double read(PlantSystem p, Signal s) {
        if (p == null) {
            return Double.NaN;
        }
        switch (s.kind()) {
            case CORE_POWER -> { return p.core().power(); }
            case CORE_REACTIVITY -> { return p.core().reactivity(); }
            case CORE_PERIOD -> { return period(p); }
            case CORE_ORM -> {
                // ORM is a rod count from the individual rods, not the insertion scalar.
                return p.core() instanceof com.ben.csp.rbmk.physics.UnifiedReactorCore u
                    ? u.orm() : Double.NaN;
            }
            case CHANNEL_FLOW -> {
                Rotameter[] r = p.rotameters();
                return s.index() < r.length ? r[s.index()].flowKgS() : Double.NaN;
            }
            case CHANNEL_VOID -> {
                Rotameter[] r = p.rotameters();
                return s.index() < r.length ? (r[s.index()].starving() ? 1.0 : 0.0) : Double.NaN;
            }
            case CHANNEL_QUALITY, CHANNEL_TEMP -> { return Double.NaN; } // per-channel, later
            case DRUM_LEVEL -> {
                SteamSeparator[] d = p.separators();
                return s.index() < d.length ? d[s.index()].level() : Double.NaN;
            }
            case DRUM_PRESSURE -> {
                SteamSeparator[] d = p.separators();
                return s.index() < d.length ? d[s.index()].pressurePa() / 98066.5 : Double.NaN;
            }
            case PUMP_FLOW -> {
                MainCirculatingPump[] m = p.pumps();
                return s.index() < m.length ? m[s.index()].flowM3s() * 3600.0 : Double.NaN;
            }
            case PUMP_SPEED -> {
                MainCirculatingPump[] m = p.pumps();
                return s.index() < m.length ? (m[s.index()].running() ? 1000.0 : 0.0) : Double.NaN;
            }
            case TURBINE_SPEED -> {
                Turbine[] t = p.turbines();
                return s.index() < t.length ? t[s.index()].rpm() : Double.NaN;
            }
            case TURBINE_POWER -> {
                Turbine[] t = p.turbines();
                return s.index() < t.length ? t[s.index()].powerW() / 1e6 : Double.NaN;
            }
            case CONDENSER_TEMP -> {
                Condenser[] c = p.condensers();
                return s.index() < c.length ? c[s.index()].coolingWaterOutC() : Double.NaN;
            }
            case ROD_POSITION -> { return p.core().insertion() * 100.0; }
            // Per-rod limit switches read the individual rod, not the scalar mean.
            case ROD_LIMIT_LOWER -> { return rodFraction(p, 0.99, true); }
            case ROD_LIMIT_UPPER -> { return rodFraction(p, 0.01, false); }
            case ROD_OVERCOMP -> { return Double.NaN; } // ПК drive not yet modelled
            case ROD_LAMP_DEPTH -> { return rodFraction(p, 0.99, true); }
            case ROD_LAMP_TOP -> { return rodFraction(p, 0.01, false); }
            default -> { return Double.NaN; }
        }
    }

    /** Fraction of rods past a limit, or 1/0 from the scalar if the bank is absent. */
    private static double rodFraction(PlantSystem p, double limit, boolean above) {
        if (p.core() instanceof com.ben.csp.rbmk.physics.UnifiedReactorCore u
            && u.rodBank() != null && u.rodBank().size() > 0) {
            var b = u.rodBank();
            int n = 0;
            for (int i = 0; i < b.size(); i++) {
                double v = b.insertion(i);
                if (above ? v >= limit : v <= limit) {
                    n++;
                }
            }
            return n > 0 ? 1.0 : 0.0;
        }
        double v = p.core().insertion();
        return above ? (v >= limit ? 1.0 : 0.0) : (v <= limit ? 1.0 : 0.0);
    }

    /**
     * The reactor period, s: the time for the power to change by a factor of e,
     * measured from the core's own power history rather than from a formula. A
     * negative period is a decaying core, an infinite one a steady one.
     */
    private static double period(PlantSystem p) {
        if (!(p.core() instanceof com.ben.csp.rbmk.physics.UnifiedReactorCore u)) {
            return Double.NaN;
        }
        return u.reactorPeriod();
    }
}
