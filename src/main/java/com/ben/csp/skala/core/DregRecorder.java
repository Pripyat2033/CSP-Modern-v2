package com.ben.csp.skala.core;

import com.ben.csp.rbmk.plant.Instrumentation;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * The DREG program — SKALA's "black box". It records three kinds of signal
 * (from the Chornobyl Family restoration):
 *
 * <ul>
 *   <li><b>Discrete</b> — states of regulators, valves, pumps (on/off).</li>
 *   <li><b>Individual analogue</b> — a value each time, e.g. a coolant
 *       thermohydraulic parameter.</li>
 *   <li><b>Switchable analogue</b> — a shared converter multiplexed over many
 *       sensors, e.g. the positions of the control rods. Reading these takes
 *       time, because the multiplexer has to be switched to each one.</li>
 * </ul>
 *
 * <p><b>The critical property, and why the Unit 4 tape is read with care:</b>
 * DREG timestamps a value at the moment it is <em>processed</em>, not the moment
 * it was measured. Under normal operation the lag is invisible; during the
 * accident it means the printout can show a state that no longer existed. This
 * class models that lag deliberately, so the reconstruction inherits the same
 * caveat the real investigators had to work around.
 */
public final class DregRecorder {

    public enum SignalType { DISCRETE, INDIVIDUAL_ANALOG, SWITCHABLE_ANALOG }

    /** One recorded line: what, when processed, and the value then. */
    public record Entry(double processedAtS, String code, SignalType type,
                        double value, String unit, String note) {}

    private final Deque<Entry> tape = new ArrayDeque<>();
    private final int capacity;
    private double simTimeS;
    private int switchableCursor;

    /** The lag a switchable-analogue point accrues per step (the multiplexer). */
    private static final double SWITCHABLE_LAG_S = 0.5;

    public DregRecorder(int capacity) {
        this.capacity = capacity;
    }

    public void tick(double dt) {
        simTimeS += dt;
    }

    public double simTimeS() { return simTimeS; }

    /**
     * Register one signal. {@code registeredAtS} is when the sensor actually
     * read the value; the entry is stamped at {@code simTimeS} (processing
     * time), and the difference is recorded as the note when it matters.
     */
    public Entry record(Instrumentation.Signal s, double value, double registeredAtS) {
        SignalType type = classify(s);
        double stamp = simTimeS;
        if (type == SignalType.SWITCHABLE_ANALOG) {
            stamp += SWITCHABLE_LAG_S; // the multiplexer has to switch to it
        }
        String note = "";
        if (type == SignalType.SWITCHABLE_ANALOG && stamp - registeredAtS > 0.05) {
            note = String.format("processed %.1f s after measurement (switchable channel)",
                stamp - registeredAtS);
        }
        Entry e = new Entry(stamp, s.code(), type, value, s.unit(), note);
        tape.addLast(e);
        while (tape.size() > capacity) {
            tape.removeFirst();
        }
        return e;
    }

    /** How DREG classifies a signal, by the instrumentation family. */
    public static SignalType classify(Instrumentation.Signal s) {
        return switch (s.kind()) {
            case ROD_POSITION -> SignalType.SWITCHABLE_ANALOG;
            case CHANNEL_FLOW, CHANNEL_VOID, CHANNEL_QUALITY, CHANNEL_TEMP,
                 DRUM_LEVEL, DRUM_PRESSURE, PUMP_FLOW, PUMP_SPEED, TURBINE_SPEED,
                 TURBINE_POWER, CONDENSER_TEMP, MCP_HEADER_FLOW, FEEDWATER_FLOW,
                 SPD -> SignalType.INDIVIDUAL_ANALOG;
            case CORE_POWER, CORE_REACTIVITY, CORE_ORM, CORE_PERIOD ->
                SignalType.DISCRETE;
        };
    }

    public List<Entry> tape() { return List.copyOf(tape); }

    public int size() { return tape.size(); }

    /** How many entries carry the processing-lag caveat (switchable channels). */
    public int laggedEntries() {
        int n = 0;
        for (Entry e : tape) {
            if (!e.note().isEmpty()) n++;
        }
        return n;
    }
}
