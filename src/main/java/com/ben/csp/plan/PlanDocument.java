package com.ben.csp.plan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A whole plan: an ordered set of layers. This is the thing a display projects;
 * it does not know or care what it depicts.
 */
public final class PlanDocument {

    private final String name;
    private final List<PlanLayer> layers = new ArrayList<>();

    public PlanDocument(String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    public String name() { return name; }

    public PlanLayer layer(String name) {
        for (PlanLayer l : layers) {
            if (l.name().equals(name)) {
                return l;
            }
        }
        PlanLayer l = new PlanLayer(name);
        layers.add(l);
        return l;
    }

    public List<PlanLayer> layers() {
        return Collections.unmodifiableList(layers);
    }

    /** Every primitive on a visible layer, in layer order. */
    public List<PlanPrimitive> visiblePrimitives() {
        List<PlanPrimitive> out = new ArrayList<>();
        for (PlanLayer l : layers) {
            if (l.isVisible()) {
                out.addAll(l.primitives());
            }
        }
        return out;
    }
}
