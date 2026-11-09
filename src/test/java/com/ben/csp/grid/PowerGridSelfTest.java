package com.ben.csp.grid;

/** Standalone self-test for the power grid: real megawatts, frequency, shedding. */
public final class PowerGridSelfTest {
    private static int checks = 0;

    /** A source that supplies a fixed number of megawatts. */
    private record FixedSource(String id, double mw) implements PowerSource {
        @Override public double availableMw() { return mw; }
    }

    public static void main(String[] args) {
        PowerGrid grid = new PowerGrid();

        // one RBMK unit is a real 1000 MW
        eq(1000.0, Power.RBMK_UNIT_MW, "an RBMK-1000 unit is 1000 MW");
        eq(50.0, Power.NOMINAL_FREQUENCY_HZ, "the USSR grid runs at 50 Hz");
        eq(50.0, grid.frequencyHz(), "the grid starts at nominal frequency");

        // a town and a factory on the grid
        BuildingConsumer town = new BuildingConsumer("town", "Town", 300, LoadPriority.NORMAL);
        BuildingConsumer factory = new BuildingConsumer("factory", "Factory", 400, LoadPriority.SHEDDABLE);
        BuildingConsumer hospital = new BuildingConsumer("hospital", "Hospital", 50, LoadPriority.ESSENTIAL);
        grid.addConsumer(town);
        grid.addConsumer(factory);
        grid.addConsumer(hospital);

        // nothing generating yet: a blackout waiting to happen
        eq(750.0, grid.totalDemandMw(), "total demand is the sum of the buildings");
        eq(0.0, grid.supplyMw(), "no supply with the plant down");

        // one unit online covers the whole town
        grid.addSource(new FixedSource("unit1", Power.RBMK_UNIT_MW));
        grid.reenergise();
        eq(1000.0, grid.supplyMw(), "one unit supplies 1000 MW");
        eq(750.0, grid.demandMw(), "all load is served");
        check(!grid.isShort(), "the grid is not short");
        grid.tick();
        check(grid.frequencyHz() > 50.0, "a surplus raises the frequency above nominal");

        // the unit trips: supply gone, frequency falls, load is shed
        PowerGrid g2 = new PowerGrid();
        g2.addSource(new FixedSource("unit1", 0.0)); // tripped
        g2.addConsumer(new BuildingConsumer("town", "Town", 300, LoadPriority.NORMAL));
        g2.addConsumer(new BuildingConsumer("factory", "Factory", 400, LoadPriority.SHEDDABLE));
        g2.addConsumer(new BuildingConsumer("hospital", "Hospital", 50, LoadPriority.ESSENTIAL));
        check(g2.isShort(), "with the unit gone the grid is short");
        for (int i = 0; i < 400 && !g2.isBlackout(); i++) {
            g2.tick();
        }
        check(g2.isBlackout(), "the grid collapses with no supply");

        // partial supply: shedding drops the least important load first
        PowerGrid g3 = new PowerGrid();
        g3.addSource(new FixedSource("unit1", 500.0)); // only half
        BuildingConsumer f3 = new BuildingConsumer("factory", "Factory", 400, LoadPriority.SHEDDABLE);
        BuildingConsumer t3 = new BuildingConsumer("town", "Town", 300, LoadPriority.NORMAL);
        BuildingConsumer h3 = new BuildingConsumer("hospital", "Hospital", 50, LoadPriority.ESSENTIAL);
        g3.addConsumer(f3);
        g3.addConsumer(t3);
        g3.addConsumer(h3);
        // drive the frequency down to the shed trigger (8 ticks of -0.125 Hz)
        for (int i = 0; i < 12; i++) {
            g3.tick();
        }
        check(!f3.isEnergised(), "the sheddable factory is dropped first");
        check(h3.isEnergised(), "the essential hospital is never shed");
        check(g3.balanceMw() >= 0, "shedding brings the grid back into balance");

        System.out.println("PowerGridSelfTest: " + checks + " checks passed.");
    }

    private static void eq(Object e, Object a, String what) {
        checks++;
        if (!String.valueOf(e).equals(String.valueOf(a))) {
            throw new AssertionError(what + ": expected <" + e + "> but was <" + a + ">");
        }
    }
    private static void check(boolean cond, String what) {
        checks++;
        if (!cond) throw new AssertionError(what);
    }
}
