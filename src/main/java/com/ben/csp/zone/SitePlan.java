package com.ben.csp.zone;

import com.ben.csp.enterprise.Enterprise;
import com.ben.csp.enterprise.Facility;
import com.ben.csp.enterprise.FacilityType;
import com.ben.csp.enterprise.Footprint;
import com.ben.csp.infra.Route;
import com.ben.csp.infra.RouteKind;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The layout the enterprises agree for a prombaza: where the rail spur comes in, where
 * the roads run, and which block each enterprise's office and facilities stand on.
 *
 * <p>The territory is not a place buildings are dropped into. It is surveyed first: a
 * rail corridor along the near edge, a road spine inside it, and each enterprise given a
 * yard off the road, its buildings set out in lines within the yard. The plan is worked
 * out once from the territory and the roster and is deterministic, so the same ground
 * always yields the same site and a saved zone rebuilds itself without the plan stored.
 */
public final class SitePlan {

    /** The gap left between plots, in metres. */
    public static final double LANE = 4.0;
    /** The width of a road. */
    public static final double ROAD_W = 6.0;
    /** The width of the rail corridor. */
    public static final double RAIL_W = 6.0;

    private final Map<String, List<Footprint>> plots = new HashMap<>();
    private final Map<String, Footprint> hq = new HashMap<>();
    private final List<Route> routes = new ArrayList<>();

    private SitePlan() {}

    /** The plot of the facility at this index in the enterprise's own order. */
    public Footprint plot(String enterpriseId, int index) {
        List<Footprint> l = plots.get(enterpriseId);
        if (l == null || index < 0 || index >= l.size()) {
            return null;
        }
        return l.get(index);
    }

    /** The plot of the enterprise's office. */
    public Footprint hqPlot(String enterpriseId) {
        return hq.get(enterpriseId);
    }

    public List<Route> routes() {
        return List.copyOf(routes);
    }

    /**
     * Survey the territory and lay the roster out on it. An enterprise that cannot be
     * given a yard is simply left out of the plan: it will wait for more ground rather
     * than spoil the site.
     */
    public static SitePlan layOut(PrombazaTerritory territory, List<Enterprise> roster) {
        SitePlan plan = new SitePlan();
        Footprint b = territory.boundary();
        double x0 = b.minX() + LANE;
        double usableW = b.width() - 2 * LANE;
        double zFront = b.minZ() + RAIL_W + ROAD_W + LANE;
        double zMax = b.maxZ() - LANE;

        // the rail corridor along the near edge, and the road spine just inside it
        double railZ = b.minZ() + RAIL_W / 2.0;
        double roadZ = b.minZ() + RAIL_W + ROAD_W / 2.0;
        plan.routes.add(new Route("rail-main", RouteKind.RAIL,
            new double[]{b.minX(), b.maxX()}, new double[]{railZ, railZ}));
        plan.routes.add(new Route("road-main", RouteKind.ROAD,
            new double[]{b.minX(), b.maxX()}, new double[]{roadZ, roadZ}));

        double maxYardW = Math.min(usableW, 100);
        double cursorX = x0;
        double bandZ = zFront;
        double bandDepth = 0;

        for (Enterprise e : roster) {
            List<Facility> facilities = e.facilities();
            List<Footprint> local = new ArrayList<>(facilities.size());
            double lx = 0, lz = 0, lineH = 0, blockW = 0;
            for (Facility f : facilities) {
                double[] s = EnterpriseLifecycle.sizeFor(f.type());
                if (lx > 0 && lx + s[0] > maxYardW) {   // this line is full
                    lz += lineH + LANE;
                    lx = 0;
                    lineH = 0;
                }
                local.add(Footprint.of(lx, lz, lx + s[0], lz + s[1]));
                lx += s[0] + LANE;
                lineH = Math.max(lineH, s[1]);
                blockW = Math.max(blockW, lx - LANE);
            }
            double blockH = lz + lineH;
            if (blockW <= 0 || blockH <= 0) {
                continue;
            }
            // start a new band if this yard will not fit beside the last one
            if (cursorX > x0 && cursorX + blockW > x0 + usableW) {
                bandZ += bandDepth + ROAD_W + LANE;
                plan.routes.add(new Route("road-band-" + Math.round(bandZ), RouteKind.ROAD,
                    new double[]{b.minX(), b.maxX()}, new double[]{bandZ - ROAD_W - LANE / 2, bandZ - ROAD_W - LANE / 2}));
                cursorX = x0;
                bandDepth = 0;
            }
            if (cursorX + blockW > x0 + usableW || bandZ + blockH > zMax) {
                continue;   // no room for this enterprise in the plan
            }
            List<Footprint> world = new ArrayList<>(local.size());
            for (Footprint p : local) {
                world.add(Footprint.of(p.minX() + cursorX, p.minZ() + bandZ,
                    p.maxX() + cursorX, p.maxZ() + bandZ));
            }
            plan.plots.put(e.id(), world);
            plan.hq.put(e.id(), findHq(e, facilities, world));
            cursorX += blockW + LANE;
            bandDepth = Math.max(bandDepth, blockH);
        }
        return plan;
    }

    private static Footprint findHq(Enterprise e, List<Facility> facilities, List<Footprint> placed) {
        for (int i = 0; i < facilities.size(); i++) {
            if (facilities.get(i).type() == FacilityType.HEADQUARTERS) {
                return placed.get(i);
            }
        }
        return placed.isEmpty() ? null : placed.get(0);
    }
}
