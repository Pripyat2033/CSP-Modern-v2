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
        ensureSupply(server, state);
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

    /**
     * Make sure every enterprise is building through the prombaza's входной
     * контроль desk. A zone restored from a save comes back without its supply
     * line, so this re-attaches it — a reloaded world must not quietly go back to
     * building with unchecked material.
     */
    private static void ensureSupply(MinecraftServer server, ZoneState state) {
        String pbId = state.zone().prombazaId();
        com.ben.csp.prombaza.PrombazaState pbs =
            com.ben.csp.prombaza.PrombazaState.get(server);
        MaterialSupply supply = new MaterialSupply(pbs.yard(pbId), pbs.control(pbId));
        state.zone().setSupply(supply);
    }

    /** Draw the whole zone on the ground: every enterprise's office and its sites. */
    public static void drawZone(ServerWorld world, ZoneEnterprises zone) {
        if (zone.plan() != null) {
            GroundWorks.drawRoutes(world, zone.plan().routes());
        }
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
