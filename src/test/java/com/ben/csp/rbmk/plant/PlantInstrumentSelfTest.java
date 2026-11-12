package com.ben.csp.rbmk.plant;

/**
 * Self-test for the plant's field instruments. It proves the valve and the meter
 * read their value from a real, stepped {@link PlantSystem} — not a constant —
 * so the number shown on the block is the number the physics holds.
 */
public final class PlantInstrumentSelfTest {

    private static int checks = 0;

    private static void check(boolean ok, String what) {
        checks++;
        if (!ok) {
            throw new AssertionError("FAILED: " + what);
        }
    }

    public static void main(String[] args) {
        // --- a dead plant: the instruments read "no plant", not a fake number ---
        int idx00 = PlantInstruments.channelIndexOf(0, 0);
        check(idx00 >= 0, "the lattice centre (0,0) is a real channel (" + idx00 + ")");
        check(PlantInstruments.channelIndexOf(9999, 9999) == -1,
            "a point far off the lattice is not a channel");

        // --- a real plant, stepped: the meter tracks the channel's own rotameter ---
        PlantSystem p = new PlantSystem(false);
        p.startUp();
        for (int i = 0; i < 200; i++) {
            p.step(0.05); // 10 s
        }

        // the meter must equal the plant's own rotameter, exactly
        double flow = PlantInstruments.channelFlowKgS(p, 0, 0);
        double reading = PlantInstruments.channelReading(p, 0, 0);
        check(flow == p.rotameters()[idx00].flowKgS(),
            "the meter reads the channel's own flow (" + flow + " kg/s)");
        check(reading == p.rotameters()[idx00].reading(),
            "the meter reads the channel's own float height");
        check(flow > 0, "a running plant has positive channel flow");

        // --- the valve reads the plant's own valve ---
        double pos = PlantInstruments.valvePosition(p, 10);
        check(pos == p.valves()[PlantInstruments.circuitOf(10)].position(),
            "the valve reads the plant's own valve position");
        check(PlantInstruments.valvePosition(p, -10)
                == p.valves()[PlantInstruments.circuitOf(-10)].position(),
            "the other circuit's valve reads its own circuit");

        // --- circuits are split by side of the reactor ---
        check(PlantInstruments.circuitOf(10) == 1, "+z channels are circuit 1");
        check(PlantInstruments.circuitOf(-10) == 0, "-z channels are circuit 0");

        // --- a starving channel reads starving ---
        p.rotameters()[idx00].setFlowKgS(0.1);
        check(PlantInstruments.channelStarving(p, 0, 0),
            "a channel below half nominal flow reads starving");
        check(PlantInstruments.channelReading(p, 0, 0) < 0.5,
            "the float drops when the channel starves");

        System.out.println("PlantInstrumentSelfTest: " + checks + " checks passed.");
    }
}
