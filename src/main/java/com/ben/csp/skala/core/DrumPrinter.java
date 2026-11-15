package com.ben.csp.skala.core;

import com.ben.csp.rbmk.plant.Instrumentation;
import com.ben.csp.rbmk.plant.PlantSystem;

import java.util.ArrayList;
import java.util.List;

/**
 * The ATsPU-128 high-speed drum printer, and the cartogram format it produced.
 *
 * <p>From the Chornobyl Family restoration: a drum with characters around its
 * surface spins at high speed, and for each print position a small
 * electromagnetic hammer strikes the paper and ribbon against the drum when the
 * wanted character passes. The result is fast, but the whole machine weighed
 * about 300 kg. There were <b>two</b> such units: one for the most important
 * parameters, one to record deviations quickly for post-event analysis.
 *
 * <p>A <b>cartogram</b> is not a list of numbers — it arranges them by the
 * physical layout of the reactor channels. Each cell is <b>four symbols wide</b>
 * and holds a few lines: the channel type, the projected 24-hour channel power,
 * the coolant flow, and the boiling-crisis margin. Because the output is very
 * large it is printed in <b>two halves</b>, left then right, with coordinate
 * rulers.
 *
 * <p>The cartogram is built from the live plant: the coolant-flow cell value is
 * read from the channel's real {@link Instrumentation} signal.
 */
public final class DrumPrinter {

    public enum Unit { PARAMETERS, DEVIATIONS }

    private final Unit unit;

    public DrumPrinter(Unit unit) {
        this.unit = unit;
    }

    public Unit unit() { return unit; }

    /**
     * Build a cartogram of the active zone from the live plant, split into the
     * left and right halves, with the channel layout preserved.
     */
    public List<String> cartogram(PlantSystem plant, List<Instrumentation.Signal> signals) {
        List<String> out = new ArrayList<>();
        out.add("PRIZMA CARTOGRAM — coolant flow by channel (halves: LEFT then RIGHT)");
        out.add("cell = 4 symbols wide; lines per cell: type / power24h / flow / margin");
        int half = 20; // channels per half-width for the printable sheet

        int[] xs = signals.stream()
            .filter(s -> s.kind() == Instrumentation.Kind.CHANNEL_FLOW)
            .mapToInt(Instrumentation.Signal::index).toArray();
        if (xs.length == 0) {
            out.add("(no channel signals)");
            return out;
        }

        for (int part = 0; part < 2; part++) {
            out.add("");
            out.add(part == 0 ? "===== LEFT HALF =====" : "===== RIGHT HALF =====");
            out.add("     " + ruler(part == 0 ? 0 : 20, half));
            for (int row = 0; row < 20; row++) {
                StringBuilder line = new StringBuilder(String.format("%4d ", row));
                for (int col = 0; col < half; col++) {
                    int idx = (part * half + col) * 20 + row;
                    double flow = channelFlow(plant, signals, idx);
                    // a compact 4-symbol cell: flow code, with a mark if starving
                    String cell = Double.isNaN(flow)
                        ? " ..."
                        : String.format("%4d", (int) Math.round(flow * 10));
                    line.append(cell);
                }
                out.add(line.toString());
            }
        }
        out.add("");
        out.add("(printed on the ATsPU-128 drum printer; ~1 min per large cartogram)");
        return out;
    }

    private static String ruler(int from, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(String.format("%4d", (from + i) % 100));
        }
        return sb.toString();
    }

    private static double channelFlow(PlantSystem plant, List<Instrumentation.Signal> signals,
                                      int channelIndex) {
        for (Instrumentation.Signal s : signals) {
            if (s.kind() == Instrumentation.Kind.CHANNEL_FLOW && s.index() == channelIndex) {
                return Instrumentation.read(plant, s);
            }
        }
        return Double.NaN;
    }

    /** The rod-position cartogram: which channels hold control/protection rods. */
    public List<String> rodMap(List<Instrumentation.Signal> signals) {
        List<String> out = new ArrayList<>();
        out.add("ROD POSITION CARTOGRAM — control & protection rods (active zone)");
        int n = 0;
        StringBuilder row = new StringBuilder();
        for (Instrumentation.Signal s : signals) {
            if (s.kind() == Instrumentation.Kind.ROD_POSITION) {
                row.append(String.format("%4d", s.index()));
                if (++n % 20 == 0) {
                    out.add(row.toString());
                    row.setLength(0);
                }
            }
        }
        if (row.length() > 0) {
            out.add(row.toString());
        }
        out.add("(" + n + " control-rod positions; printed as a cartogram)");
        return out;
    }
}
