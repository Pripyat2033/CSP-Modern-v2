package com.ben.csp.zone.genplan;

import com.ben.csp.enterprise.Enterprise;
import com.ben.csp.enterprise.Facility;
import com.ben.csp.enterprise.FacilityType;
import com.ben.csp.enterprise.Footprint;
import com.ben.csp.infra.Route;
import com.ben.csp.infra.RouteKind;
import com.ben.csp.zone.EnterpriseLifecycle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Works out a general plan (генплан) for the prombaza the way a Soviet project
 * institute did it, from the norms in {@link SiteNorms} (СНиП II-89-80*).
 *
 * <p>The site is laid out by function, not by owner. The pre-plant zone of offices
 * faces the entrance; the production zone of plants and workshops stands in the
 * middle; the storage zone backs onto the rail; the transport depots stand in the
 * auxiliary zone by the roads. Roads run between the zones and down the spine, and
 * the rail spur runs along the far edge where the goods come in. Every gap between
 * buildings is a fire break and every road is a проезд: the figures are the norm's,
 * not the mod's.
 *
 * <p>The plan is deterministic — the same ground and the same roster always yield
 * the same site — so a saved zone rebuilds itself without the plan being stored.
 */
public final class GenplanZones {

    /** The zones in the order they are set out from the entrance to the rail. */
    private static final Zone[] ORDER = {
        Zone.PREDZAVODSKAYA, Zone.PRODUCTION, Zone.STORAGE, Zone.AUXILIARY
    };

    private final Map<String, Footprint[]> plots = new HashMap<>();
    private final Map<String, Footprint> hq = new HashMap<>();
    private final List<Route> routes = new ArrayList<>();
    private final Map<Zone, Integer> zoneCounts = new LinkedHashMap<>();

    private double builtArea;
    private double siteArea;

    private GenplanZones() {}

    /** The plot of the facility at this index in the enterprise's own order, or null. */
    public Footprint plot(String enterpriseId, int index) {
        Footprint[] a = plots.get(enterpriseId);
        if (a == null || index < 0 || index >= a.length) {
            return null;
        }
        return a[index];
    }

    /** The plot of the enterprise's office, or null if it was not placed. */
    public Footprint hqPlot(String enterpriseId) {
        return hq.get(enterpriseId);
    }

    public List<Route> routes() {
        return List.copyOf(routes);
    }

    /** How many facilities each zone holds. */
    public Map<Zone, Integer> zoneCounts() {
        return Map.copyOf(zoneCounts);
    }

    /** The share of the site covered by buildings, against the norm's minimum. */
    public double density() {
        return siteArea > 0 ? builtArea / siteArea : 0;
    }

    /** The share of the site the norm requires to be built on. */
    public double densityRequired() {
        return SiteNorms.DENSITY_MIN;
    }

    /**
     * Set the roster out on the ground by zone. A facility the site cannot hold is
     * left unplaced and its enterprise waits for more ground.
     */
    public static GenplanZones layOut(Footprint boundary, List<Enterprise> roster) {
        GenplanZones g = new GenplanZones();
        g.siteArea = boundary.area();
        double x0 = boundary.minX() + SiteNorms.LANE;
        double usableW = boundary.width() - 2 * SiteNorms.LANE;
        double zCursor = boundary.minZ() + SiteNorms.ROAD_TWO_LANE + SiteNorms.LANE;
        double zBottom = boundary.maxZ() - SiteNorms.RAIL_CORRIDOR - SiteNorms.LANE;

        for (Enterprise e : roster) {
            g.plots.put(e.id(), new Footprint[e.facilities().size()]);
        }

        // The rail spur along the far edge, where the goods come in, and a siding to
        // the stores; the front road where the people come in; and a spine joining them.
        double railZ = boundary.maxZ() - SiteNorms.RAIL_CORRIDOR / 2.0;
        double frontZ = boundary.minZ() + SiteNorms.ROAD_TWO_LANE / 2.0;
        double spineX = boundary.minX() + SiteNorms.LANE + SiteNorms.ROAD_TWO_LANE / 2.0;
        g.routes.add(new Route("rail-spur", RouteKind.RAIL,
            new double[]{boundary.minX(), boundary.maxX()}, new double[]{railZ, railZ}));
        g.routes.add(new Route("road-front", RouteKind.ROAD,
            new double[]{boundary.minX(), boundary.maxX()}, new double[]{frontZ, frontZ}));
        g.routes.add(new Route("road-spine", RouteKind.ROAD,
            new double[]{spineX, spineX}, new double[]{frontZ, railZ}));

        boolean firstZone = true;
        for (Zone zone : ORDER) {
            List<Lot> lots = collect(roster, zone);
            if (lots.isEmpty()) {
                continue;
            }
            if (!firstZone) {
                // a road between the zones, set back from the last row by the fire break
                double roadZ = zCursor + SiteNorms.SANITARY_BREAK / 2.0;
                g.routes.add(new Route("road-zone-" + zone.name().toLowerCase(), RouteKind.ROAD,
                    new double[]{boundary.minX(), boundary.maxX()}, new double[]{roadZ, roadZ}));
                zCursor += SiteNorms.SANITARY_BREAK;
            }
            firstZone = false;

            double cursorX = x0;
            double rowH = 0;
            for (Lot lot : lots) {
                if (cursorX > x0 && cursorX + lot.w > x0 + usableW) {
                    zCursor += rowH + SiteNorms.rowGap(zone);   // next row of this zone
                    cursorX = x0;
                    rowH = 0;
                }
                if (cursorX + lot.w > x0 + usableW || zCursor + lot.h > zBottom) {
                    continue;   // no room on the site: this facility waits for more ground
                }
                Footprint f = Footprint.of(cursorX, zCursor, cursorX + lot.w, zCursor + lot.h);
                g.plots.get(lot.entId)[lot.index] = f;
                if (lot.type == FacilityType.HEADQUARTERS) {
                    g.hq.put(lot.entId, f);
                }
                g.builtArea += f.area();
                g.zoneCounts.merge(zone, 1, Integer::sum);
                cursorX += lot.w + SiteNorms.LANE;
                rowH = Math.max(rowH, lot.h);
            }
            zCursor += rowH;   // the next zone begins below this one
        }

        g.fillMissingHq(roster);
        return g;
    }

    /** If an enterprise's office was not the HQ building, stand it on its first plot. */
    private void fillMissingHq(List<Enterprise> roster) {
        for (Enterprise e : roster) {
            if (hq.containsKey(e.id())) {
                continue;
            }
            Footprint[] a = plots.get(e.id());
            if (a == null) {
                continue;
            }
            for (Footprint f : a) {
                if (f != null) {
                    hq.put(e.id(), f);
                    break;
                }
            }
        }
    }

    private static List<Lot> collect(List<Enterprise> roster, Zone zone) {
        List<Lot> out = new ArrayList<>();
        for (Enterprise e : roster) {
            List<Facility> fs = e.facilities();
            for (int i = 0; i < fs.size(); i++) {
                FacilityType t = fs.get(i).type();
                if (Zone.of(t) != zone) {
                    continue;
                }
                double[] s = EnterpriseLifecycle.sizeFor(t);
                out.add(new Lot(e.id(), i, t, s[0], s[1]));
            }
        }
        // the big plants first, so a zone packs tidily; ties broken for determinism
        out.sort(Comparator.comparingDouble((Lot l) -> -(l.w * l.h))
            .thenComparing(l -> l.entId).thenComparingInt(l -> l.index));
        return out;
    }

    private record Lot(String entId, int index, FacilityType type, double w, double h) {}
}
