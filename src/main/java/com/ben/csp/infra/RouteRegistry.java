package com.ben.csp.infra;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** The routes of the site: roads, rail spurs and the buried services. */
public final class RouteRegistry {

    private final List<Route> routes = new ArrayList<>();

    public Route add(Route route) {
        Objects.requireNonNull(route, "route");
        if (byId(route.id()).isPresent()) {
            throw new IllegalArgumentException("a route with id " + route.id() + " already exists");
        }
        routes.add(route);
        return route;
    }

    public Optional<Route> byId(String id) {
        return routes.stream().filter(r -> r.id().equals(id)).findFirst();
    }

    public boolean remove(String id) {
        return routes.removeIf(r -> r.id().equals(id));
    }

    public List<Route> all() {
        return Collections.unmodifiableList(routes);
    }

    /** Every route of one kind. */
    public List<Route> ofKind(RouteKind kind) {
        return routes.stream().filter(r -> r.kind() == kind).toList();
    }

    /** Every route whose sheet group is the given group. */
    public List<Route> ofGroup(SheetGroup group) {
        return routes.stream().filter(r -> r.sheetGroup() == group).toList();
    }

    public boolean isEmpty() { return routes.isEmpty(); }
    public int size() { return routes.size(); }
}
