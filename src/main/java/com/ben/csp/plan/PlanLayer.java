package com.ben.csp.plan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A named layer of a plan — the drafting equivalent of an overlay (survey,
 * boundary, plots, utilities). Layers can be shown or hidden, and are drawn in
 * order.
 */
public final class PlanLayer {

    private final String name;
    private final List<PlanPrimitive> primitives = new ArrayList<>();
    private boolean visible = true;

    public PlanLayer(String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    public String name() { return name; }
    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }

    public PlanLayer add(PlanPrimitive primitive) {
        primitives.add(Objects.requireNonNull(primitive, "primitive"));
        return this;
    }

    public List<PlanPrimitive> primitives() {
        return Collections.unmodifiableList(primitives);
    }

    public void clear() {
        primitives.clear();
    }
}
