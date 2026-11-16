package com.ben.csp.zone;

import com.ben.csp.enterprise.Footprint;
import com.ben.csp.perf.PerformanceProbe;
import com.ben.csp.prombaza.Area;
import com.ben.csp.prombaza.GoodsYard;
import com.ben.csp.prombaza.Prombaza;
import com.ben.csp.prombaza.PrombazaControl;
import com.ben.csp.prombaza.PrombazaState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.List;

/**
 * The zone's operations, in one place, so the same work can be done two ways: by
 * the operator's commands, and — the way that matters — by the Director talking to
 * the staff. The command and the spoken word must never drift apart, so both call
 * the methods here and send the lines they return.
 *
 * <p>Nothing here knows how it was invoked. It takes the server, a place, and who
 * asked, and it returns the lines to be said. In 1970 nobody typed a command into a
 * computer to start a power station; the interface was people. This is the engine
 * under that interface.
 */
public final class ZoneOperations {

    private ZoneOperations() {}

    /** The outcome of an operation: whether it worked, and what to say about it. */
    public record Result(boolean ok, List<String> lines) {
        public static Result of(boolean ok, String... lines) {
            return new Result(ok, List.of(lines));
        }
    }

    /**
     * Stand the enterprises up. The ground is the last designated prombaza if there
     * is one; otherwise a square of the given radius around {@code at}, for a trial.
     * On success a secretary is put on the ground, so there is always someone at the
     * site to speak to.
     */
    public static Result foundSite(MinecraftServer server, ServerWorld world,
                                   BlockPos at, int radius, String actor) {
        ZoneState state = ZoneState.get(server);
        if (state.isSpawned()) {
            return Result.of(false, "The zone is already standing.");
        }
        List<String> lines = new ArrayList<>();
        Area area;
        var prombazas = PrombazaState.get(server).registry();
        if (!prombazas.isEmpty()) {
            Prombaza pb = prombazas.all().get(prombazas.all().size() - 1);
            area = pb.area();
            lines.add("Building on the designated prombaza '" + pb.id() + "'.");
        } else {
            area = Area.of(at.getX() - radius, at.getY(), at.getZ() - radius,
                           at.getX() + radius, at.getY() + 20, at.getZ() + radius);
            lines.add("No prombaza designated: laying out a " + (radius * 2)
                + " m territory here.");
        }
        try {
            state.spawn(area);
        } catch (IllegalStateException e) {
            return Result.of(false, "Could not stand the enterprises up: " + e.getMessage()
                + ". The ten enterprises need room for ten offices and their facilities.");
        }
        state.markDirty();

        // The supply line: the rail siding and the входной контроль desk, so every
        // delivery is checked before it reaches a site.
        PrombazaState pbs = PrombazaState.get(server);
        String pbId = state.zone().prombazaId();
        state.zone().setSupply(new MaterialSupply(pbs.yard(pbId), pbs.control(pbId)));
        pbs.markDirty();

        lines.add(state.zone().count() + " construction enterprises have come onto the territory.");
        lines.add("Each will build its office, hire staff, then build its facilities — a marker "
            + "first, then a building over time.");

        BlockPos office = surfaceSpot(world, state.territory().boundary());
        if (Muster.placeSecretary(world, office)) {
            lines.add("A secretary has taken the office. Speak to her to run the zone — no "
                + "commands are needed.");
        }
        return new Result(true, lines);
    }

    /** How far each enterprise has come, and how the site is laid out. */
    public static Result siteReport(MinecraftServer server) {
        ZoneState state = ZoneState.get(server);
        if (!state.isSpawned()) {
            return Result.of(false, "The zone is not standing yet.");
        }
        List<String> lines = new ArrayList<>();
        ZoneEnterprises zone = state.zone();
        lines.add("Day " + state.day() + ": " + zone.operationalCount() + "/" + zone.count()
            + " enterprises operational.");
        SitePlan plan = zone.plan();
        if (plan != null) {
            StringBuilder zones = new StringBuilder();
            plan.zoneCounts().forEach((z, n) ->
                zones.append(z.russian()).append(" ").append(n).append("; "));
            lines.add("The site is surveyed to СНиП II-89-80*: " + plan.routes().size()
                + " route(s) laid out (the rail spur and the roads). Zones — "
                + zones.toString().trim());
            lines.add("Building density " + Math.round(plan.density() * 100) + "% "
                + "(the norm requires at least " + Math.round(plan.densityRequired() * 100) + "%).");
        }
        for (EnterpriseLifecycle l : zone.lifecycles()) {
            lines.add("  " + l.enterprise().name() + " [" + l.enterprise().specialty().label()
                + "] — " + l.progressLabel());
        }
        return new Result(true, lines);
    }

    /** Put the whole workforce on the ground. A negative limit means all of them. */
    public static Result muster(MinecraftServer server, ServerWorld world, BlockPos at, int limit) {
        ZoneState state = ZoneState.get(server);
        if (!state.isSpawned()) {
            return Result.of(false, "The zone is not standing yet.");
        }
        PerformanceProbe.resetPeak();
        long started = System.nanoTime();
        int placed = Muster.everyone(world, state.zone(), at, limit);
        long ms = (System.nanoTime() - started) / 1_000_000L;
        return Result.of(true, "Muster: " + placed + " workers on the ground in " + ms
            + " ms. " + Muster.count(world) + " now standing.");
    }

    /** The входной контроль desk: what it received, what it did, what it cost. */
    public static Result prombazaReport(MinecraftServer server, String id) {
        PrombazaState state = PrombazaState.get(server);
        if (state.registry().byId(id).isEmpty()) {
            return Result.of(false, "No prombaza with id '" + id + "'.");
        }
        PrombazaControl desk = state.control(id);
        List<String> lines = new ArrayList<>();
        lines.add("Входной контроль at prombaza '" + id + "':");
        if (desk.receipts().isEmpty()) {
            lines.add("  No deliveries received yet. The desk checks each batch as it arrives.");
            return new Result(true, lines);
        }
        for (String line : desk.report()) {
            lines.add("  " + line);
        }
        lines.add("  cleared for building: " + desk.cleared()
            + " | held (rework/return/hold): " + desk.held()
            + " | installed with a LATENT defect: " + desk.latent());
        lines.add("  inspection cost: " + Math.round(desk.inspectionManHours())
            + " man-hours -> build takes x" + String.format("%.2f", desk.buildTimeMultiplier())
            + " as long");
        return new Result(true, lines);
    }

    /** Work up to {@code n} standing wagons through the desk. The backlog rushes it. */
    public static Result workWagons(MinecraftServer server, String id, int n, long day) {
        PrombazaState state = PrombazaState.get(server);
        if (state.registry().byId(id).isEmpty()) {
            return Result.of(false, "No prombaza with id '" + id + "'.");
        }
        GoodsYard yard = state.yard(id);
        PrombazaControl desk = state.control(id);
        int before = yard.standingCount();
        int cleared = yard.clearUpTo(n, desk, day, 1.0);
        state.markDirty();
        if (cleared == 0) {
            return Result.of(false, "Nothing standing at prombaza '" + id + "'.");
        }
        return Result.of(true,
            "Worked " + cleared + " wagon(s) through входной контроль at '" + id
                + "' (backlog was " + before + ").",
            "  desk now: cleared " + desk.cleared() + " | held " + desk.held()
                + " | latent " + desk.latent()
                + " | inspection " + Math.round(desk.inspectionManHours()) + " man-hours");
    }

    /** A dry spot on the surface at the centre of a plot. */
    private static BlockPos surfaceSpot(ServerWorld world, Footprint fp) {
        int x = (int) Math.floor(fp.centerX());
        int z = (int) Math.floor(fp.centerZ());
        int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
        return new BlockPos(x, y, z);
    }
}
