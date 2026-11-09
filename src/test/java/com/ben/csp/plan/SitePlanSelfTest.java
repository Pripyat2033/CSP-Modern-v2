package com.ben.csp.plan;

import com.ben.csp.enterprise.Enterprise;
import com.ben.csp.enterprise.Facility;
import com.ben.csp.enterprise.FacilityType;
import com.ben.csp.enterprise.Footprint;
import com.ben.csp.enterprise.Specialty;
import com.ben.csp.infra.Route;
import com.ben.csp.infra.RouteKind;
import com.ben.csp.infra.RouteRegistry;
import com.ben.csp.infra.SheetGroup;
import com.ben.csp.prombaza.Area;
import com.ben.csp.prombaza.Prombaza;

import java.util.List;

/** Standalone self-test for the site plan (boundary + facilities + routes). */
public final class SitePlanSelfTest {
    private static int checks = 0;

    public static void main(String[] args) {
        Prombaza prombaza = new Prombaza("pb", "Central", Area.of(0, 60, 0, 200, 80, 200), 1, "Ben");

        Facility hq = new Facility("hq", FacilityType.HEADQUARTERS, "HQ", 5);
        hq.site(Footprint.of(20, 20, 60, 50));
        Facility plant = new Facility("cp", FacilityType.CONCRETE_PLANT, "Concrete plant", 10);
        plant.site(Footprint.of(120, 130, 180, 180));
        Facility unsited = new Facility("u", FacilityType.WORKSHOP, "Un-sited shop", 2);

        Enterprise ent = new Enterprise("e1", "Construction Trust", Specialty.GENERAL_CONSTRUCTION,
            List.of(hq, plant, unsited));

        RouteRegistry routes = new RouteRegistry();
        routes.add(new Route("road1", RouteKind.ROAD,
            new double[] {60, 90, 120}, new double[] {35, 35, 155}, "hq", "cp"));
        routes.add(new Route("rail1", RouteKind.RAIL,
            new double[] {60, 200}, new double[] {35, 35}, null, null));
        routes.add(new Route("pipe1", RouteKind.PIPE,
            new double[] {60, 90, 120}, new double[] {40, 40, 140}, null, null));

        SitePlanSource src = new SitePlanSource(prombaza, List.of(ent), routes);

        // the whole plan has the boundary, two sited buildings, and three routes
        PlanDocument all = src.buildAll();
        long boundary = all.layer("boundary").primitives().size();
        long facilities = all.layer("facilities").primitives().stream()
            .filter(p -> p instanceof PlanPrimitive.Rect).count();
        long routeLines = all.layer("routes").primitives().size();
        eq(1L, boundary, "one boundary");
        eq(2L, facilities, "only sited facilities are drawn");
        eq(3L, routeLines, "all three routes");

        // the transport sheet has only roads and rail
        PlanDocument transport = src.build(SheetGroup.TRANSPORT);
        eq(2L, (long) transport.layer("routes").primitives().size(),
            "transport sheet has the road and the rail");
        eq(2L, (long) transport.layer("facilities").primitives().stream()
            .filter(p -> p instanceof PlanPrimitive.Rect).count(),
            "the base buildings are on every sheet");

        // the utilities sheet has only the pipe
        PlanDocument utilities = src.build(SheetGroup.UTILITIES);
        eq(1L, (long) utilities.layer("routes").primitives().size(),
            "utilities sheet has only the pipe");

        // a route is a polyline in world coordinates
        PlanPrimitive route0 = transport.layer("routes").primitives().get(0);
        check(route0 instanceof PlanPrimitive.Polyline, "routes are polylines");
        PlanPrimitive.Polyline pl = (PlanPrimitive.Polyline) route0;
        eq(60.0, pl.xs()[0], "the road starts at the HQ x");

        // the extent covers the prombaza
        PlanExtent e = src.extent();
        eq(0.0, e.minX(), "extent starts at the boundary");
        eq(200.0, e.maxX(), "extent ends at the boundary");

        // and it rasterises without error
        RasterPlanRenderer r = new RasterPlanRenderer(128, 128);
        PlanSheet sheet = new PlanSheet("1", "Site plan", Scale.S1_1000, src.extent(), all,
            new TitleBlock("Central", "Site plan", Scale.S1_1000, "1", 1, "g", "b", 1));
        r.begin(sheet.viewFor(128, 128, 4));
        r.drawAll(sheet.document().visiblePrimitives());
        r.end();
        int drawn = 0;
        for (int px : r.pixels()) if (px != 0) drawn++;
        check(drawn > 0, "the site plan draws something");

        System.out.println("SitePlanSelfTest: " + checks + " checks passed.");
    }

    private static void eq(Object e, Object a, String what) {
        checks++;
        if (!String.valueOf(e).equals(String.valueOf(a))) {
            throw new AssertionError(what + ": expected <" + e + "> but was <" + a + ">");
        }
    }
    private static void check(boolean cond, String what) {
        checks++;
        if (!cond) throw new AssertionError(what);
    }
}
