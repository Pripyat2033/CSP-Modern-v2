package com.ben.csp.skala.core;

import com.ben.csp.rbmk.plant.PlantSystem;
import com.ben.csp.rbmk.plant.PlantTicker;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

/**
 * Runs the {@link SkalaComplex} on the server clock, wired to the live plant.
 *
 * <p>This is the "register into SKALA" step made operational: every tick the
 * complex's DREG loop reads a slice of the plant's instrumentation register and
 * records it, so the machine's data is the plant's data. The plant is discovered
 * through {@link PlantTicker}; if the plant is not running the complex simply
 * idles, exactly as a real SKALA with no live inputs would.
 */
public final class SkalaTicker {

    private static SkalaComplex complex;
    private static long lastNs;

    private SkalaTicker() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(SkalaTicker::tick);
    }

    public static SkalaComplex complex() {
        if (complex == null) {
            complex = new SkalaComplex();
            complex.boot();
        }
        return complex;
    }

    /** Start the operational set, as the operators would on a shift handover. */
    public static SkalaComplex start() {
        SkalaComplex c = complex();
        c.start();
        return c;
    }

    public static void halt() {
        if (complex != null) {
            complex.halt();
        }
    }

    private static void tick(MinecraftServer server) {
        if (complex == null || !complex.running()) {
            return;
        }
        long now = System.nanoTime();
        if (lastNs == 0) {
            lastNs = now;
            return;
        }
        double dt = (now - lastNs) / 1e9;
        lastNs = now;
        if (dt <= 0 || dt > 1.0) {
            dt = 0.05;
        }
        PlantSystem plant = PlantTicker.plant();
        complex.tick(dt, plant);
    }
}
