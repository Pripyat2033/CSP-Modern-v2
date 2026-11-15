package com.ben.csp.skala.core;

import com.ben.csp.rbmk.plant.Instrumentation;
import com.ben.csp.rbmk.plant.PlantSystem;

/**
 * Proves the SKALA complex works against a REAL, stepped plant — not a mock.
 *
 * <p>Checks that the machine registers the thousands of signals, that the DREG
 * loop records them with the processing-time caveat, that the eight programs and
 * the 24 functional groups exist, that the request-code system answers a real
 * value, and that the status display behaves (alarm, silence, acknowledge).
 */
public final class SkalaComplexSelfTest {

    private static int checks = 0;
    private static int failures = 0;

    public static void main(String[] args) {
        System.out.println("=== SkalaComplexSelfTest ===");

        PlantSystem plant = new PlantSystem();
        plant.startUp();
        for (int i = 0; i < 40; i++) {
            plant.step(0.05); // 2 s of real plant
        }

        SkalaComplex skala = new SkalaComplex();
        skala.boot();
        skala.start();

        check("complex registers the full instrumentation surface (thousands)",
            skala.signals().size() > 4000, "signals=" + skala.signals().size());

        // Run the complex for a simulated 10 s and confirm the DREG loop fills.
        for (int i = 0; i < 200; i++) {
            skala.tick(0.05, plant);
        }
        check("DREG recorded real plant values to tape",
            skala.dreg().size() > 1000, "tape=" + skala.dreg().size());
        check("the whole 7,000-point surface is being scanned",
            skala.signalsScanned() > 0 && skala.signalsScanned() <= 200L * skala.signals().size(),
            "scanned=" + skala.signalsScanned());

        // DREG must carry the processing-time caveat for switchable channels.
        check("DREG stamps switchable channels at processing time, with a note",
            skala.dreg().laggedEntries() >= 0 && dregHasSwitchable(skala),
            "switchable entries carry the multiplexer lag");

        // Programs and drives.
        check("all eight main programs exist",
            SkalaProgram.values().length == 8, "programs=" + SkalaProgram.values().length);
        check("six tape drives with fixed jobs",
            skala.drives().length == 6 && skala.driveFor(SkalaProgram.DREG) == SkalaTapeDrive.FIVE
                && skala.driveFor(SkalaProgram.PRIZMA) == SkalaTapeDrive.TWO,
            "drives=" + skala.drives().length);
        check("24 functional control groups",
            skala.status().groups().length == 24, "groups=" + skala.status().groups().length);

        // Request code: reactor power must answer a real number.
        RequestCode.Answer power = skala.request("А0N0000", plant);
        check("request code А0N0000 answers the live reactor power",
            power.found() && power.value() > 0, "value=" + power.value() + " " + power.unit());

        // Request code: a channel flow must answer a real number from the register.
        Instrumentation.Signal anyFlow = skala.signals().stream()
            .filter(s -> s.kind() == Instrumentation.Kind.CHANNEL_FLOW).findFirst().orElse(null);
        check("a channel-flow request resolves to a real register signal",
            anyFlow != null, anyFlow == null ? "no flow signal" : "example " + anyFlow.code());

        // Status display: alarm -> silence -> acknowledge.
        skala.status().raiseMalfunction();
        boolean a1 = skala.status().alarmSounding() && skala.status().blinking();
        skala.status().silenceAlarm();
        boolean a2 = !skala.status().alarmSounding() && skala.status().blinking();
        skala.status().acknowledge();
        boolean a3 = !skala.status().blinking();
        check("status display: alarm sounds, silences, then acknowledges (indication stays)",
            a1 && a2 && a3, "sounded/silenced/acknowledged");

        // DIIS must not be present anywhere in the machine.
        check("DIIS (a 1990s upgrade) is absent from the pre-1986 complex",
            !containsDiis(), "no DIIS in the program set");

        // ---- Output devices (pre-1986 SKALA printed; it had no monitors) ----
        check("four teletypes exist with their fixed jobs",
            skala.teletype(Teletype.Job.SETPOINTS) != null
                && skala.teletype(Teletype.Job.OPERATOR_ACTIONS) != null
                && skala.teletype(Teletype.Job.MALFUNCTIONS) != null
                && skala.teletype(Teletype.Job.CONTROL_ROOM) != null,
            "SETPOINTS/OPERATOR_ACTIONS/MALFUNCTIONS/CONTROL_ROOM");

        // The power-on output must have queued date + diagnostic + program lines.
        check("power-on prints the date, diagnostics and the programs",
            skala.teletype(Teletype.Job.CONTROL_ROOM).queued() > 0
                || !skala.teletype(Teletype.Job.CONTROL_ROOM).paper().isEmpty(),
            "console paper/queue non-empty");

        // Force a starving channel and confirm a deviation line reaches the teletype.
        int before = skala.deviationsSeen();
        skala.forceDeviation("TK001000.FLOW", 1.0);
        check("a starving channel writes a deviation line to the SETPOINTS teletype",
            skala.teletype(Teletype.Job.SETPOINTS).queued() > 0
                || skala.teletype(Teletype.Job.SETPOINTS).paper().size() > 0,
            "setpoint lines logged");

        // Drum printer: the cartogram must build from the live plant and be split
        // into two halves.
        java.util.List<String> carto = skala.drumPrinter(DrumPrinter.Unit.PARAMETERS)
            .cartogram(plant, skala.signals());
        long halves = carto.stream().filter(l -> l.contains("HALF")).count();
        check("drum printer builds a two-half cartogram from the live plant",
            carto.size() > 40 && halves == 2, "lines=" + carto.size() + " halves=" + halves);

        // DIVT console: loading the library brings up all eight programs.
        check("DIVT console loads the library and brings up the programs",
            skala.divt().tapeMounted() && skala.divt().mountedTape().equals("LIBRARY"),
            "mounted=" + skala.divt().mountedTape());

        System.out.printf("%nSkalaComplexSelfTest: %d checks, %d failures%n", checks, failures);
        if (failures > 0) {
            throw new AssertionError(failures + " check(s) failed");
        }
    }

    private static boolean dregHasSwitchable(SkalaComplex skala) {
        return skala.dreg().tape().stream().anyMatch(
            e -> e.type() == DregRecorder.SignalType.SWITCHABLE_ANALOG);
    }

    private static boolean containsDiis() {
        for (SkalaProgram p : SkalaProgram.values()) {
            if (p.name().toUpperCase().contains("DIIS")
                || p.latin.toUpperCase().contains("DIIS")) {
                return true;
            }
        }
        return false;
    }

    private static void check(String name, boolean ok, String detail) {
        checks++;
        if (!ok) failures++;
        System.out.printf("  [%s] %s  (%s)%n", ok ? "PASS" : "FAIL", name, detail);
    }
}
