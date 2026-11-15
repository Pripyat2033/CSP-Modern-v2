package com.ben.csp.skala.core;

import com.ben.csp.rbmk.plant.Instrumentation;
import com.ben.csp.rbmk.plant.PlantSystem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The SKALA complex: the pre-1986 computer that watched the reactor. This is the
 * whole machine in one place, and it is Minecraft-free so it can be exercised
 * standalone against a real, stepped plant.
 *
 * <p>What it does, and where each piece comes from (Chornobyl Family restoration
 * of Unit 3's surviving machine):
 * <ul>
 *   <li>registers the plant's {@link Instrumentation} signals — the analogue
 *       front end (thousands of points, not a dozen);</li>
 *   <li>runs the single pre-1986 data-acquisition loop, <b>DREG</b>, which
 *       records discrete, individual-analogue and switchable-analogue signals —
 *       with the real caveat that a value is stamped when <em>processed</em>, not
 *       when measured;</li>
 *   <li>runs the periodic control programs ({@link SkalaProgram}): Kerva/KCTK/KIKD
 *       once a minute, RAO continuously, PRIZMA periodically;</li>
 *   <li>answers <b>request codes</b> from a request device ({@link RequestCode});</li>
 *   <li>drives the {@link SkalaStatusDisplay} and its 24 {@link FunctionalGroup}s;</li>
 *   <li>manages the six {@link SkalaTapeDrive}s.</li>
 * </ul>
 *
 * <p>DIIS is deliberately absent: it was a 1990s upgrade, so it did not exist in
 * April 1986 and could not have contributed to that night.
 */
public final class SkalaComplex {

    /** Register a value, keyed by signal code, as the loops scan it. */
    private final List<Instrumentation.Signal> signals = Instrumentation.all();
    private final Map<String, Double> scanned = new HashMap<>(8192);

    private final DregRecorder dreg = new DregRecorder(200_000);
    private final SkalaStatusDisplay status = new SkalaStatusDisplay();
    private final FunctionalGroup[] groups = status.groups();
    private final SkalaTapeDrive[] drives = SkalaTapeDrive.values();

    /** The DREG loop covers every point in about 5 s (the machine's cadence). */
    private static final double DREG_FULL_SCAN_S = 5.0;
    private static final double MINUTE = 60.0;

    private double timeS;
    private int scanCursor;
    private final double[] programDue = new double[SkalaProgram.COUNT];

    private boolean running;
    private long signalsScanned;
    private int deviationsSeen;

    public SkalaComplex() {
        for (int i = 0; i < programDue.length; i++) {
            programDue[i] = MINUTE; // minute-period programs first fire after a minute
        }
        status.setProcessorA(SkalaStatusDisplay.ProcessorState.HALTED);
        status.setProcessorB(SkalaStatusDisplay.ProcessorState.HALTED);
    }

    public List<Instrumentation.Signal> signals() { return signals; }
    public DregRecorder dreg() { return dreg; }
    public SkalaStatusDisplay status() { return status; }
    public SkalaTapeDrive[] drives() { return drives; }
    public boolean running() { return running; }
    public long signalsScanned() { return signalsScanned; }
    public int deviationsSeen() { return deviationsSeen; }
    public double timeS() { return timeS; }

    /** Power-on: processors go ready and the main programs load from tape 1. */
    public void boot() {
        status.setProcessorA(SkalaStatusDisplay.ProcessorState.READY);
        status.setProcessorB(SkalaStatusDisplay.ProcessorState.READY);
    }

    /** Start the operational set (configuration 2.0): both processors running. */
    public void start() {
        status.setProcessorA(SkalaStatusDisplay.ProcessorState.RUNNING);
        status.setProcessorB(SkalaStatusDisplay.ProcessorState.RUNNING);
        status.setConfiguration(SkalaStatusDisplay.Configuration.PAIRED_2_0);
        running = true;
    }

    public void halt() {
        running = false;
        status.setProcessorA(SkalaStatusDisplay.ProcessorState.HALTED);
        status.setProcessorB(SkalaStatusDisplay.ProcessorState.HALTED);
    }

    /** One tick of the whole complex. */
    public void tick(double dt, PlantSystem plant) {
        if (!running) return;
        timeS += dt;
        dreg.tick(dt);

        runDregScan(plant);
        runPeriodicPrograms(plant);
    }

    /**
     * The single pre-1986 acquisition loop. Each tick it scans a slice of the
     * register, so the whole 7,000-point surface is refreshed about every 5 s,
     * and every scanned value is written to the DREG tape.
     */
    private void runDregScan(PlantSystem plant) {
        int perTick = Math.max(1, (int) Math.ceil(signals.size() / (DREG_FULL_SCAN_S * 20.0)));
        for (int i = 0; i < perTick; i++) {
            Instrumentation.Signal s = signals.get(scanCursor);
            scanCursor = (scanCursor + 1) % signals.size();
            double v = Instrumentation.read(plant, s);
            if (!Double.isNaN(v)) {
                scanned.put(s.code(), v);
                signalsScanned++;
                dreg.record(s, v, timeS);
            }
        }
    }

    /**
     * The periodic control programs. Kerva/KCTK/KIKD fire once a minute; RAO
     * watches continuously. Each program scans its own family of signals through
     * the functional groups and raises a deviation if a value leaves its band.
     */
    private void runPeriodicPrograms(PlantSystem plant) {
        // Kerva — coolant flow through the active zone (once a minute).
        if (due(SkalaProgram.KRV, 1.0)) {
            scanFamily(plant, Instrumentation.Kind.CHANNEL_FLOW, groups[0]);
        }
        // KCTK — channel integrity (once a minute). Leak detectors are not yet
        // in the register, so the group is exercised but no deviation is raised.
        if (due(SkalaProgram.KCTK, 1.0)) {
            scanFamily(plant, Instrumentation.Kind.CHANNEL_TEMP, groups[6]);
        }
        // KIKD — detector function check (once a minute).
        if (due(SkalaProgram.KIKD, 1.0)) {
            scanFamily(plant, Instrumentation.Kind.SPD, groups[12]);
        }
        // RAO — emergency deviations, continuously (rod positions).
        if (due(SkalaProgram.RAO, 1.0 / 20.0)) {
            scanFamily(plant, Instrumentation.Kind.ROD_POSITION, groups[12]);
        }
    }

    private boolean due(SkalaProgram p, double everySeconds) {
        programDue[p.ordinal()] -= 1.0 / 20.0;
        if (programDue[p.ordinal()] <= 0) {
            programDue[p.ordinal()] = everySeconds;
            return true;
        }
        return false;
    }

    /** Scan one family of signals through a group, and note any deviation. */
    private void scanFamily(PlantSystem plant, Instrumentation.Kind kind,
                            FunctionalGroup group) {
        group.setState(FunctionalGroup.State.BUSY);
        for (Instrumentation.Signal s : signals) {
            if (s.kind() != kind) continue;
            double v = Instrumentation.read(plant, s);
            if (Double.isNaN(v)) continue;
            scanned.put(s.code(), v);
            signalsScanned++;
            if (kind == Instrumentation.Kind.CHANNEL_FLOW && v < 4.0) {
                deviationsSeen++;
                if (deviationsSeen == 1) {
                    status.raiseMalfunction();
                }
            }
        }
        group.setState(FunctionalGroup.State.IDLE);
    }

    /**
     * Answer a request entered on a request device. Resolves the code against the
     * live plant; if the plant is not running, falls back to the last scanned
     * value so the device still answers.
     */
    public RequestCode.Answer request(String code, PlantSystem plant) {
        RequestCode rc = RequestCode.parse(code);
        if (rc == null) {
            return RequestCode.Answer.notFound(code, "malformed code");
        }
        RequestCode.Answer a = rc.resolve(plant, signals);
        if (!a.found() && scanned.containsKey(rc.toString())) {
            return new RequestCode.Answer(true, rc.toString(), scanned.get(rc.toString()),
                "", "last scanned value (plant not running)", false, "");
        }
        return a;
    }

    /** The last value scanned for a code, or NaN. */
    public double lastScanned(String code) {
        return scanned.getOrDefault(code, Double.NaN);
    }

    /** The tape drive whose job matches a program (1,2,3,4,5,6 indexing). */
    public SkalaTapeDrive driveFor(SkalaProgram p) {
        return switch (p) {
            case PRIZMA -> SkalaTapeDrive.TWO;
            case DREG -> SkalaTapeDrive.FIVE;
            default -> SkalaTapeDrive.ONE;
        };
    }

    public List<String> tapeInventory() {
        List<String> out = new ArrayList<>();
        for (SkalaTapeDrive d : drives) {
            out.add(d.label + ": " + d.job + " — " + d.description);
        }
        return out;
    }
}
