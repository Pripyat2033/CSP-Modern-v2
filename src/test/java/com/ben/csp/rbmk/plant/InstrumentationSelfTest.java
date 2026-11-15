package com.ben.csp.rbmk.plant;

import com.ben.csp.rbmk.RbmkLattice;

/**
 * Proves the plant's control surface is the REAL one, not a handful of lumped
 * numbers. A real RBMK-1000 fed the SKALA computer about 4,000 data input points
 * (INSAG-7 §2.5); this test asserts the instrumentation register enumerates the
 * per-channel, per-rod, per-drum, per-pump and detector signals that make up that
 * surface, and that the count is in the right order of magnitude.
 */
public final class InstrumentationSelfTest {

    private static int checks = 0;
    private static int failures = 0;

    public static void main(String[] args) {
        System.out.println("=== InstrumentationSelfTest ===");

        int total = Instrumentation.size();
        check("instrumentation enumerates thousands of signals, not a dozen",
            total > 4000, "total=" + total);

        var counts = Instrumentation.counts();
        int fuel = RbmkLattice.fuelCount();
        int control = RbmkLattice.controlCount();
        check("one coolant-flow signal per fuel channel (the rotameters)",
            counts.getOrDefault(Instrumentation.Kind.CHANNEL_FLOW, 0) == fuel,
            "flow=" + counts.getOrDefault(Instrumentation.Kind.CHANNEL_FLOW, 0)
                + " fuelChannels=" + fuel);
        check("one rod-position signal per control channel",
            counts.getOrDefault(Instrumentation.Kind.ROD_POSITION, 0) == control,
            "rods=" + counts.getOrDefault(Instrumentation.Kind.ROD_POSITION, 0)
                + " controlChannels=" + control);
        check("in-core power-distribution detectors are present (PPDDCS)",
            counts.getOrDefault(Instrumentation.Kind.SPD, 0) >= 100,
            "spd=" + counts.getOrDefault(Instrumentation.Kind.SPD, 0));
        check("drums, pumps and turbines each report their signals",
            counts.getOrDefault(Instrumentation.Kind.DRUM_LEVEL, 0) == 4
                && counts.getOrDefault(Instrumentation.Kind.PUMP_FLOW, 0) == 8
                && counts.getOrDefault(Instrumentation.Kind.TURBINE_SPEED, 0) == 2,
            "drums=" + counts.getOrDefault(Instrumentation.Kind.DRUM_LEVEL, 0)
                + " pumps=" + counts.getOrDefault(Instrumentation.Kind.PUMP_FLOW, 0)
                + " turbines=" + counts.getOrDefault(Instrumentation.Kind.TURBINE_SPEED, 0));

        // Every signal carries a source address and a unit — an untraceable
        // signal is not allowed, the same rule as the equipment register.
        long addressed = Instrumentation.all().stream()
            .filter(s -> s.code() != null && !s.code().isBlank()
                      && s.unit() != null && !s.unit().isBlank())
            .count();
        check("every signal has a code and a unit (nothing untraceable)",
            addressed == total, "addressed=" + addressed + "/" + total);

        System.out.printf("%nInstrumentationSelfTest: %d checks, %d failures%n",
            checks, failures);
        if (failures > 0) {
            throw new AssertionError(failures + " check(s) failed");
        }
    }

    private static void check(String name, boolean ok, String detail) {
        checks++;
        if (!ok) {
            failures++;
        }
        System.out.printf("  [%s] %s  (%s)%n", ok ? "PASS" : "FAIL", name, detail);
    }
}
