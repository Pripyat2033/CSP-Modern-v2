package com.ben.csp.rbmk.plant;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import com.ben.csp.rbmk.physics.SpatialReactorCore;

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
        return startPlant(false);
    }

    /** Start the plant, choosing the core: lumped (false) or full 3D (true). */
    public static PlantSystem startPlant(boolean spatial) {
        plant = new PlantSystem(spatial);
        plant.startUp();
        lastTickNs = 0;
        return plant;
    }

    /**
     * Start the plant on the TIME-DEPENDENT spatial core: the flux over the real
     * lattice is the state, stepped at 1 ms on its own thread. This is the live
     * test of "can Minecraft carry a fully spatial RBMK core".
     */
    public static PlantSystem startPlantTransient() {
        plant = new PlantSystem(new com.ben.csp.rbmk.physics.TransientReactorCore());
        plant.startUp();
        lastTickNs = 0;
        return plant;
    }

    /** True when the running plant is driven by the full 3D spatial solver. */
    public static boolean isSpatial() {
        return plant != null && plant.core() instanceof SpatialReactorCore;
    }

    public static void stopPlant() {
        if (plant != null) {
            plant.shutdown();
        }
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
