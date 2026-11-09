package com.ben.csp.infra;

import java.util.Objects;

/**
 * A connection between two points of the site — a road, a rail spur, a pipe run.
 * The path is a polyline in plan coordinates (X and Z). A route may name the
 * facilities it joins, but it does not have to: a service run can simply follow
 * a line of the site.
 */
public final class Route {

    private final String id;
    private final RouteKind kind;
    private final double[] xs;
    private final double[] zs;
    private final String fromFacilityId; // may be null
    private final String toFacilityId;   // may be null

    /** A route that joins no named facilities (a service run along the site). */
    public Route(String id, RouteKind kind, double[] xs, double[] zs) {
        this(id, kind, xs, zs, null, null);
    }

    public Route(String id, RouteKind kind, double[] xs, double[] zs,
                 String fromFacilityId, String toFacilityId) {
        this.id = Objects.requireNonNull(id, "id");
        this.kind = Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(xs, "xs");
        Objects.requireNonNull(zs, "zs");
        if (xs.length < 2 || xs.length != zs.length) {
            throw new IllegalArgumentException("a route needs at least two points, x and z of equal length");
        }
        this.xs = xs.clone();
        this.zs = zs.clone();
        this.fromFacilityId = fromFacilityId;
        this.toFacilityId = toFacilityId;
    }

    public String id() { return id; }
    public RouteKind kind() { return kind; }
    public double[] xs() { return xs.clone(); }
    public double[] zs() { return zs.clone(); }
    public int pointCount() { return xs.length; }
    public double x(int i) { return xs[i]; }
    public double z(int i) { return zs[i]; }
    public String fromFacilityId() { return fromFacilityId; }
    public String toFacilityId() { return toFacilityId; }

    /** The length of the route in plan units (metres, at 1:1). */
    public double length() {
        double total = 0;
        for (int i = 0; i < xs.length - 1; i++) {
            total += Math.hypot(xs[i + 1] - xs[i], zs[i + 1] - zs[i]);
        }
        return total;
    }

    /** The group whose sheet this route is drawn on. */
    public SheetGroup sheetGroup() {
        return kind.group();
    }

    @Override
    public String toString() {
        return kind.label() + " route #" + id + " (" + pointCount() + " points, "
            + Math.round(length()) + " m)";
    }
}
