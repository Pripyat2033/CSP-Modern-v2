package com.ben.csp.rbmk.plant;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

/**
 * Runs the whole plant on the server clock: one {@link PlantSystem} advanced
 * every tick, so the reactor, the pumps, the drums, the turbines and the
 * condensers all evolve live and continuously — not in a one-shot chat command.
 *
 * <p>This is the Director's point made operational: the plant is a set of
 * running systems, and you watch them hold a steady state (or fail to).
 */
public final class PlantTicker {

    private static PlantSystem plant;
    /** Accumulated wall time, so we step in real seconds regardless of tick rate. */
    private static long lastTickNs = 0;

    private PlantTicker() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(PlantTicker::tick);
    }

    public static PlantSystem plant() {
        return plant;
    }

    public static PlantSystem startPlant() {
        plant = new PlantSystem();
        plant.startUp();
        lastTickNs = 0;
        return plant;
    }

    public static void stopPlant() {
        plant = null;
    }

    private static void tick(MinecraftServer server) {
        if (plant == null) {
            return;
        }
        long now = System.nanoTime();
        if (lastTickNs == 0) {
            lastTickNs = now;
            return;
        }
        double dt = (now - lastTickNs) / 1e9;
        lastTickNs = now;
        if (dt <= 0 || dt > 1.0) {
            dt = 0.05;
        }
        plant.step(dt);
    }
}
