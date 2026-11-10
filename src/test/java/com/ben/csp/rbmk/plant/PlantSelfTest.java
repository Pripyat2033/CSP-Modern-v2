package com.ben.csp.rbmk.plant;

/**
 * Checks the whole plant holds together: the heat balance closes, the drums hold
 * level at power, the turbines make ~1,000 MWe, and the trip matrix fires. These
 * are the checks a chat command cannot stand in for.
 */
public final class PlantSelfTest {

    private static int checks = 0;

    public static void main(String[] args) {
        nominalFlows();
        heatBalance();
        steadyState();
        trips();
        eccs();
        subsystems();
        System.out.println("PlantSelfTest: " + checks + " checks passed.");
    }

    private static void subsystems() {
        // valves
        Valve gate = new Valve(Valve.Kind.GATE);
        gate.setPosition(0.0);
        check(gate.closed(), "a gate valve closes fully");
        gate.setPosition(1.0);
        check(gate.open(), "a gate valve opens fully");
        Valve control = new Valve(Valve.Kind.CONTROL);
        control.setPosition(0.0);
        for (int i = 0; i < 100; i++) {
            control.step(0.1);
        }
        check(control.position() < 0.05, "a control valve throttles toward closed");

        // rotameters: one per fuel channel
        check(Rotameter.COUNT == 1872, "there is a rotameter on every fuel channel");
        Rotameter rot = new Rotameter(0);
        rot.setFlowKgS(0.1);
        check(rot.starving(), "a channel with almost no flow reads as starving");
        rot.setFlowKgS(Rotameter.NOMINAL_KGS);
        check(!rot.starving(), "a channel at nominal flow reads normal");

        // oil system: the standby pump starts on a turbine trip
        OilSystem oil = new OilSystem();
        check(oil.mainPumpRunning(), "the main oil pump runs at power");
        oil.onTurbineTrip(0.2);
        check(!oil.mainPumpRunning() && oil.emergencyPumpRunning(),
            "the emergency oil pump starts when the turbine slows");
        for (int i = 0; i < 100; i++) {
            oil.step(0.1);
        }
        check(!oil.lowPressureTrip(), "oil pressure is held above the trip");

        // the running plant has 6 pumps running, 2 standby
        PlantSystem p = new PlantSystem();
        int running = 0, standby = 0;
        for (MainCirculatingPump pump : p.pumps()) {
            if (pump.running()) {
                running++;
            }
            if (pump.standby()) {
                standby++;
            }
        }
        check(running == 6 && standby == 2,
            "the plant runs six pumps with two standby (got " + running + "+" + standby + ")");
    }

    private static void nominalFlows() {
        double steam = PlantPhysics.steamFlowKgS(1.0);
        near(1500.0, steam, 60.0, "nominal steam flow is ~1,500 kg/s (5,400 t/h)");
        double q = PlantPhysics.coreQuality(1.0);
        near(0.15, q, 0.02, "nominal core steam quality is ~15%");
        double flow = PlantPhysics.MCP_FLOW_CIRCUIT_M3S * 3600;
        near(24000.0, flow, 1.0, "each circuit moves 24,000 m3/h (3 pumps x 8,000)");
    }

    private static void heatBalance() {
        // electrical out + heat rejected should account for the thermal input
        PlantSystem p = new PlantSystem();
        p.startUp();
        for (int i = 0; i < 600; i++) {
            p.step(0.1);
        }
        double thermal = PlantPhysics.P_THERMAL_W * p.core().power();
        double electrical = p.electricalOutputW();
        double rejected = 0;
        for (Condenser c : p.condensers()) {
            rejected += c.heatRejectedW();
        }
        check(electrical > 800e6 && electrical < 1100e6,
            "both turbines make ~1,000 MWe together (" + fmt(electrical / 1e6) + " MW)");
        check(rejected > 0.5 * thermal,
            "the condensers reject the bulk of the heat (" + fmt(rejected / 1e6) + " MW)");
        double balance = (electrical + rejected) / thermal;
        check(balance > 0.8 && balance < 1.4,
            "electrical + rejected tracks thermal input (ratio " + fmt(balance) + ")");
    }

    private static void steadyState() {
        PlantSystem p = new PlantSystem();
        p.startUp();
        for (int i = 0; i < 600; i++) {
            p.step(0.1);
        }
        check(!p.scrammed(), "the plant runs 60 s at power without tripping");
        double minLevel = 1, maxLevel = 0;
        for (SteamSeparator d : p.separators()) {
            minLevel = Math.min(minLevel, d.level());
            maxLevel = Math.max(maxLevel, d.level());
        }
        check(minLevel > 0.1 && maxLevel < 0.9,
            "the four drums hold level (" + fmt(minLevel) + "-" + fmt(maxLevel) + ")");
        check(p.core().power() > 0.5,
            "the core stays at power (" + fmt(p.core().power()) + ")");
    }

    private static void trips() {
        // --- separator level trips (unit level) ---
        SteamSeparator hi = new SteamSeparator(0);
        // feedwater far above what can leave as steam drives the level up
        for (int i = 0; i < 5000 && !hi.highLevelTrip(); i++) {
            hi.step(0.1, 2500, 0.15, 380, 800);
        }
        check(hi.highLevelTrip(), "a drum overfilled with feedwater reaches the high trip");

        SteamSeparator lo = new SteamSeparator(0);
        // no feedwater at all drains the drum
        for (int i = 0; i < 5000 && !lo.lowLevelTrip(); i++) {
            lo.step(0.1, 2500, 0.15, 380, 0);
        }
        check(lo.lowLevelTrip(), "a drum starved of feedwater reaches the low trip");

        // --- plant level trips ---
        // loss of two pumps on one side
        PlantSystem q = new PlantSystem();
        q.startUp();
        for (int i = 0; i < 200; i++) {
            q.step(0.1);
        }
        q.pumps()[0].trip();
        q.pumps()[1].trip();
        q.step(0.1);
        check(q.scrammed(), "losing two pumps on one side trips the reactor");

        // scram drops the rods and fires the ECCS
        PlantSystem r = new PlantSystem();
        r.startUp();
        for (int i = 0; i < 100; i++) {
            r.step(0.1);
        }
        double before = r.core().insertion();
        r.scram("test");
        for (int i = 0; i < 300; i++) {
            r.step(0.1);
        }
        check(r.scrammed(), "scram latches");
        check(r.eccs().actuated(), "scram fires the ECCS accumulators");
        check(r.core().insertion() > before, "scram drives the rods in (insertion rises)");
    }

    private static void eccs() {
        Eccs e = new Eccs();
        check(e.remainingWaterKg() == 12 * Eccs.ACCUMULATOR_WATER_KG,
            "the ECCS starts with all twelve accumulators full");
        e.actuate();
        e.startLongTermPumps();
        for (int i = 0; i < 1500; i++) { // 150 s at 0.1 s
            e.step(0.1);
        }
        check(e.remainingWaterKg() < 0.1 * 12 * Eccs.ACCUMULATOR_WATER_KG,
            "the accumulators empty after ~100 s of injection");
        check(e.injectionKgS() > 0, "the long-term pumps keep injecting");
    }

    // ------------------------------------------------------------- helpers
    private static void near(double want, double got, double tol, String msg) {
        check(Math.abs(want - got) <= tol, msg + " (got " + fmt(got) + ")");
    }

    private static void check(boolean ok, String msg) {
        checks++;
        System.out.println((ok ? "  ok   " : "  FAIL ") + msg);
        if (!ok) {
            throw new AssertionError(msg);
        }
    }

    private static String fmt(double v) {
        return String.format("%.3f", v);
    }
}
