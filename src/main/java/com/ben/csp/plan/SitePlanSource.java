package com.ben.csp.plan;

import com.ben.csp.enterprise.Enterprise;
import com.ben.csp.enterprise.Facility;
import com.ben.csp.enterprise.Footprint;
import com.ben.csp.infra.Route;
import com.ben.csp.infra.RouteRegistry;
import com.ben.csp.infra.SheetGroup;
import com.ben.csp.prombaza.Area;
import com.ben.csp.prombaza.Prombaza;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Turns a prombaza into a site plan: its boundary, the facilities that stand in
 * it, and their infrastructure. One plan is built per sheet group, so the roads
 * and rail go on the transport sheet and the buried services on the utilities
 * sheet, each over the same base of boundary and buildings.
 */
public final class SitePlanSource {

    private static final int BOUNDARY = 0xFF1A1A1A;
    private static final int FACILITY = 0xFF333333;
    private static final int ROAD = 0xFF6B4A2B;
    private static final int RAIL = 0xFF444444;
    private static final int PIPE = 0xFF2E6DA4;
    private static final int CABLE = 0xFF7A4FA3;

    private final Prombaza prombaza;
    private final List<Enterprise> enterprises;
    private final RouteRegistry routes;

    public SitePlanSource(Prombaza prombaza, List<Enterprise> enterprises, RouteRegistry routes) {
        this.prombaza = Objects.requireNonNull(prombaza, "prombaza");
        this.enterprises = Objects.requireNonNull(enterprises, "enterprises");
        this.routes = Objects.requireNonNull(routes, "routes");
    }

    /** The plan for one sheet group: base + that group's routes. */
    public PlanDocument build(SheetGroup group) {
        PlanDocument doc = new PlanDocument(prombaza.name() + " — " + group.english());
        addBase(doc);
        PlanLayer routesLayer = doc.layer("routes");
        for (Route r : routes.ofGroup(group)) {
            routesLayer.add(new PlanPrimitive.Polyline(r.xs(), r.zs(), false,
                PlanStyle.of(routeColour(r), 2.0)));
        }
        return doc;
    }

    /** The whole site plan (all routes), for a single combined drawing. */
    public PlanDocument buildAll() {
        PlanDocument doc = new PlanDocument(prombaza.name());
        addBase(doc);
        PlanLayer routesLayer = doc.layer("routes");
        for (Route r : routes.all()) {
            routesLayer.add(new PlanPrimitive.Polyline(r.xs(), r.zs(), false,
                PlanStyle.of(routeColour(r), 2.0)));
        }
        return doc;
    }

    private void addBase(PlanDocument doc) {
        Area a = prombaza.area();
        doc.layer("boundary").add(new PlanPrimitive.Polyline(
            new double[] {a.minX(), a.maxX(), a.maxX(), a.minX()},
            new double[] {a.minZ(), a.minZ(), a.maxZ(), a.maxZ()},
            true, PlanStyle.of(BOUNDARY, 2.0)));

        PlanLayer buildings = doc.layer("facilities");
        for (Enterprise e : enterprises) {
            for (Facility f : e.facilities()) {
                Footprint fp = f.footprint();
                if (fp == null) {
                    continue;
                }
                buildings.add(new PlanPrimitive.Rect(fp.minX(), fp.minZ(),
                    fp.width(), fp.height(), false, PlanStyle.of(FACILITY, 2.0)));
                buildings.add(new PlanPrimitive.Label(fp.centerX(), fp.centerZ(),
                    f.name(), PlanStyle.of(FACILITY)));
            }
        }
    }

    private static int routeColour(Route r) {
        return switch (r.kind()) {
            case ROAD -> ROAD;
            case RAIL -> RAIL;
            case PIPE, WATER, SEWER -> PIPE;
            case CABLE, POWER_LINE -> CABLE;
        };
    }

    /** The extent the plans are drawn over: the prombaza boundary. */
    public PlanExtent extent() {
        Area a = prombaza.area();
        return new PlanExtent(a.minX(), a.minZ(), a.maxX(), a.maxZ());
    }
}
