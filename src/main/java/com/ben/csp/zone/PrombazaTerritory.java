package com.ben.csp.zone;

import com.ben.csp.enterprise.Footprint;
import com.ben.csp.infra.Route;
import com.ben.csp.prombaza.Area;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The prombaza territory: the ground the enterprises sit on, laid out FIRST from
 * the plans. It has a boundary, and it reserves the parts of itself that are not
 * free to build on — the roads, the rail spurs, the yards and depots. The
 * enterprises do not own this land; they come onto it and pick plots inside it.
 */
public final class PrombazaTerritory {

    private final String id;
    private final Footprint boundary;
    private final List<Footprint> reserved = new ArrayList<>();
    private final List<Route> routes = new ArrayList<>();

    public PrombazaTerritory(String id, Footprint boundary) {
        this.id = Objects.requireNonNull(id, "id");
        this.boundary = Objects.requireNonNull(boundary, "boundary");
    }

    public String id() { return id; }
    public Footprint boundary() { return boundary; }
    public double area() { return boundary.area(); }

    /** Reserve a part of the territory that is not free to build on. */
    public PrombazaTerritory reserve(Footprint plot) {
        reserved.add(Objects.requireNonNull(plot, "plot"));
        return this;
    }

    /** Record an infrastructure route (road, rail spur, pipeway) on the territory. */
    public PrombazaTerritory addRoute(Route route) {
        routes.add(Objects.requireNonNull(route, "route"));
        return this;
    }

    public List<Footprint> reserved() {
        return Collections.unmodifiableList(reserved);
    }

    public List<Route> routes() {
        return Collections.unmodifiableList(routes);
    }

    /** True if the whole plot lies inside the boundary. */
    public boolean contains(Footprint f) {
        return f.minX() >= boundary.minX() && f.maxX() <= boundary.maxX()
            && f.minZ() >= boundary.minZ() && f.maxZ() <= boundary.maxZ();
    }

    /** True if the plot overlaps any reserved part of the territory. */
    public boolean isReserved(Footprint f) {
        for (Footprint r : reserved) {
            if (overlaps(f, r)) {
                return true;
            }
        }
        return false;
    }

    static boolean overlaps(Footprint a, Footprint b) {
        return a.minX() < b.maxX() && b.minX() < a.maxX()
            && a.minZ() < b.maxZ() && b.minZ() < a.maxZ();
    }

    /** A territory from a designated prombaza area. */
    public static PrombazaTerritory fromArea(String id, Area area) {
        return new PrombazaTerritory(id, Footprint.of(
            area.minX(), area.minZ(), area.maxX(), area.maxZ()));
    }

    /**
     * A territory laid out from the plans. A prombaza is a territory — the ground and
     * its boundary — an organisational concept, not a structure. Nothing is reserved
     * and nothing is drawn on it, so the enterprises lay their plots out freely inside
     * the boundary, which keeps even narrow ground usable.
     */
    public static PrombazaTerritory standard(String id, Area area) {
        return fromArea(id, area);
    }

    @Override
    public String toString() {
        return "территория " + id + " " + boundary.width() + "x" + boundary.height()
            + " m, " + reserved.size() + " reserved, " + routes.size() + " routes";
    }
}
