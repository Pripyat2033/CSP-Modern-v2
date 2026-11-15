package com.ben.csp.skala.core;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * An RDA-80 teletype — one of the paper output devices of SKALA.
 *
 * <p>From the Chornobyl Family restoration: each machine could drive up to four
 * teletypes, and their jobs were fixed:
 * <ul>
 *   <li><b>SETPOINTS</b> — monotonous messages about reaching or exceeding the
 *       set points of technological parameters.</li>
 *   <li><b>OPERATOR_ACTIONS</b> — what the operators did.</li>
 *   <li><b>MALFUNCTIONS</b> — system malfunctions.</li>
 *   <li><b>CONTROL_ROOM</b> — the one in the control room, which operators used
 *       to obtain requested information.</li>
 *   <li><b>RESERVE</b> — spare.</li>
 * </ul>
 *
 * <p>The RDA-80 prints <b>symbol by symbol</b>, so a line takes on the order of
 * ten seconds. That slowness is not a defect to hide — it is the experience the
 * operators had ("you have to wait while the computer literally prints its
 * answer"), so it is modelled: lines queue and are emitted at a fixed rate.
 *
 * <p>The teletype is a paper log. The paper stays, which is why SKALA printed
 * rather than displayed — the evidence outlives the machine.
 */
public final class Teletype {

    public enum Job { SETPOINTS, OPERATOR_ACTIONS, MALFUNCTIONS, CONTROL_ROOM, RESERVE }

    /** One printed line, with the simulated time it came off the head. */
    public record Line(double timeS, String text) {}

    private final Job job;
    private final Deque<String> queue = new ArrayDeque<>();
    private final Deque<Line> paper = new ArrayDeque<>();
    private final int paperCapacity;

    /** Seconds per line: the RDA-80's symbol-by-symbol speed (~10 s). */
    private static final double LINE_SECONDS = 10.0;

    private double nextLineAtS;
    private double timeS;
    private boolean restricted;

    public Teletype(Job job, int paperCapacity) {
        this.job = job;
        this.paperCapacity = paperCapacity;
    }

    public Job job() { return job; }
    public boolean restricted() { return restricted; }

    /** Restrict software access to this teletype (a status-display condition). */
    public void setRestricted(boolean r) { this.restricted = r; }

    /** Queue a line. If the device is restricted, the line is not accepted. */
    public boolean print(String text) {
        if (restricted) {
            return false;
        }
        queue.addLast(text);
        return true;
    }

    /** Advance the printer head. */
    public void tick(double dt) {
        timeS += dt;
        if (!queue.isEmpty() && timeS >= nextLineAtS) {
            String t = queue.removeFirst();
            paper.addLast(new Line(timeS, t));
            while (paper.size() > paperCapacity) {
                paper.removeFirst();
            }
            nextLineAtS = timeS + LINE_SECONDS;
        }
    }

    public int queued() { return queue.size(); }
    public List<Line> paper() { return List.copyOf(paper); }

    /** Format a deviation line exactly as the source describes it: time,
     *  whether the deviation appeared or disappeared, the channel code, the
     *  coolant flow, and the amount of deviation. */
    public static String deviation(double clockS, boolean appeared, String code,
                                   double value, String unit, double deviation) {
        long t = (long) clockS;
        return String.format("%02d:%02d:%02d  DEVIATION %s  %s  %s=%.2f %s  %+.2f",
            t / 3600, (t / 60) % 60, t % 60,
            appeared ? "APPEARED" : "CLEARED", code, code.endsWith(".FLOW") ? "G" : "V",
            value, unit, deviation);
    }

    /** A generic dated line, as the machine writes at power-on. */
    public static String dated(double clockS, String text) {
        long t = (long) clockS;
        return String.format("%02d:%02d:%02d  %s", t / 3600, (t / 60) % 60, t % 60, text);
    }
}
