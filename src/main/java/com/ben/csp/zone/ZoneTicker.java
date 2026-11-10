package com.ben.csp.zone;

import com.ben.csp.zone.ground.GroundWorks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

/**
 * Runs the zone on the server clock. The enterprises build themselves while the
 * world is running, not on a command: the ticker accumulates server ticks and
 * advances one work-day every {@link #TICKS_PER_DAY}, then redraws the ground so
 * the offices and the facilities can be seen growing.
 */
public final class ZoneTicker {

    private ZoneTicker() {}

    /** Server ticks per work-day. Ten seconds a day, so a build can be watched. */
    public static final int TICKS_PER_DAY = 200;

    public static void tick(MinecraftServer server) {
        ZoneState state = ZoneState.get(server);
        if (!state.isSpawned()) {
            return;
        }
        int acc = state.tickAccumulator() + 1;
        if (acc < TICKS_PER_DAY) {
            state.setTickAccumulator(acc);
            return;
        }
        state.setTickAccumulator(0);
        state.advanceDay();
        state.markDirty();

        ServerWorld world = server.getOverworld();
        drawZone(world, state.zone());
        CrewMuster.spawnCrews(world, state.zone());
    }

    /** Draw the whole zone on the ground: every enterprise's office and its sites. */
    public static void drawZone(ServerWorld world, ZoneEnterprises zone) {
        for (EnterpriseLifecycle l : zone.lifecycles()) {
            if (l.hqPlot() != null) {
                GroundWorks.drawHq(world, l.hqPlot(), l.setup().hqProgress(),
                    l.enterprise().name());
            }
            for (ConstructionSite s : l.sites()) {
                GroundWorks.drawSite(world, s);
            }
        }
    }
}
