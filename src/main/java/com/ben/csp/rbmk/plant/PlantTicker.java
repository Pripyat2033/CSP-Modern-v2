package com.ben.csp.rbmk.plant;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import com.ben.csp.rbmk.physics.UnifiedReactorCore;

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
        return startUnified();
    }

    /**
     * Start the plant on THE core (the unified spatial core). The {@code spatial}
     * flag is retained for source compatibility and ignored: there is only one core.
     *
     * @deprecated use {@link #startPlant()}.
     */
    @Deprecated
    public static PlantSystem startPlant(boolean spatial) {
        return startUnified();
    }

    /**
     * Start the plant on THE core: the unified time-dependent spatial core with the
     * feedback loop closed (void, temperature and xenon feeding back through the
     * cross sections). This is the default plant.
     */
    public static PlantSystem startUnified() {
        plant = new PlantSystem(new UnifiedReactorCore());
        plant.startUp();
        lastTickNs = 0;
        return plant;
    }

    /**
     * {@code /rbmk plant start spatial} — retained for muscle memory. The
     * open-loop {@code TransientReactorCore} it used to launch is retired: it
     * could not feed back on itself (frozen void/temperature, no xenon), so it
     * could not reproduce the accident. It is no longer instantiable from here;
     * there is one core.
     *
     * @deprecated use {@link #startUnified()}.
     */
    @Deprecated
    public static PlantSystem startPlantTransient() {
        return startUnified();
    }

    /** True when the running plant is driven by the unified spatial core. */
    public static boolean isSpatial() {
        return plant != null && plant.core() instanceof UnifiedReactorCore;
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
