package com.ben.csp.infra;

/** Standalone self-test for routes and their sheet grouping. */
public final class RouteSelfTest {
    private static int checks = 0;

    public static void main(String[] args) {
        // kinds know their sheet group
        eq(SheetGroup.TRANSPORT, RouteKind.ROAD.group(), "roads are transport");
        eq(SheetGroup.TRANSPORT, RouteKind.RAIL.group(), "rail is transport");
        eq(SheetGroup.UTILITIES, RouteKind.PIPE.group(), "pipes are utilities");
        eq(SheetGroup.UTILITIES, RouteKind.CABLE.group(), "cables are utilities");
        eq("\u0421\u0435\u0442\u0438", SheetGroup.UTILITIES.russian(), "utilities sheet is named in Russian");

        // a road between two facilities
        Route road = new Route("r1", RouteKind.ROAD,
            new double[] {0, 100, 100}, new double[] {0, 0, 60}, "hq", "concrete");
        eq(3, road.pointCount(), "three points");
        eq(100.0 + 60.0, road.length(), "length is the sum of the segments");
        eq(SheetGroup.TRANSPORT, road.sheetGroup(), "the road is drawn on the transport sheet");
        eq("hq", road.fromFacilityId(), "it names the facility it leaves");

        // a pipe
        Route pipe = new Route("p1", RouteKind.PIPE,
            new double[] {0, 50}, new double[] {0, 50}, null, null);
        eq(SheetGroup.UTILITIES, pipe.sheetGroup(), "the pipe is drawn on the utilities sheet");
        check(pipe.fromFacilityId() == null, "a service run need not name a facility");

        // the registry sorts by kind and by group
        RouteRegistry reg = new RouteRegistry();
        reg.add(road);
        reg.add(pipe);
        reg.add(new Route("r2", RouteKind.RAIL, new double[] {0, 40}, new double[] {0, 40}, null, null));
        eq(3, reg.size(), "three routes");
        eq(2, reg.ofGroup(SheetGroup.TRANSPORT).size(), "two transport routes");
        eq(1, reg.ofGroup(SheetGroup.UTILITIES).size(), "one utility route");
        eq(1, reg.ofKind(RouteKind.RAIL).size(), "one rail route");

        // guards
        expectThrow(() -> reg.add(road), "cannot add a duplicate id");
        expectThrow(() -> new Route("bad", RouteKind.ROAD, new double[] {1}, new double[] {1}),
            "a route needs two points");
        expectThrow(() -> new Route("bad", RouteKind.ROAD, new double[] {1, 2}, new double[] {1}),
            "x and z must be the same length");

        // removing
        check(reg.remove("p1"), "a route can be removed");
        eq(2, reg.size(), "two left");

        System.out.println("RouteSelfTest: " + checks + " checks passed.");
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
    private static void expectThrow(Runnable r, String what) {
        checks++;
        try { r.run(); } catch (RuntimeException e) { return; }
        throw new AssertionError(what + ": expected an exception");
    }
}
