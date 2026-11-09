package com.ben.csp.plan;

import com.ben.csp.prombaza.Prombaza;
import com.ben.csp.prombaza.PrombazaRegistry;

import java.util.Objects;
import java.util.Optional;

/**
 * Turns the prombazas of the zone into a plan. This is the concrete source that
 * feeds the display; a survey, a construction site or a utility network would be
 * other sources feeding the same generic display.
 */
public final class PrombazaPlanSource implements PlanSource {

    private final PrombazaRegistry registry;
    private final String documentName;

    public PrombazaPlanSource(PrombazaRegistry registry, String documentName) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.documentName = Objects.requireNonNull(documentName, "documentName");
    }

    @Override
    public Optional<PlanDocument> buildPlan() {
        if (registry.isEmpty()) {
            return Optional.empty();
        }
        PlanDocument doc = new PlanDocument(documentName);
        PlanLayer boundary = doc.layer("prombaza-boundary");
        PlanLayer labels = doc.layer("prombaza-labels");
        for (Prombaza p : registry.all()) {
            int minX = p.area().minX();
            int minZ = p.area().minZ();
            int maxX = p.area().maxX() + 1;
            int maxZ = p.area().maxZ() + 1;
            boundary.add(new PlanPrimitive.Polyline(
                new double[] {minX, maxX, maxX, minX},
                new double[] {minZ, minZ, maxZ, maxZ},
                true, PlanStyle.of(0xFF202020, 2.0)));
            labels.add(new PlanPrimitive.Label(
                p.area().centerX(), p.area().centerZ(), p.name(),
                PlanStyle.of(0xFF404040)));
        }
        return Optional.of(doc);
    }
}
