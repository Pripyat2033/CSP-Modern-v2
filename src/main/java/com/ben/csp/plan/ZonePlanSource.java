package com.ben.csp.plan;

import com.ben.csp.enterprise.Facility;
import com.ben.csp.enterprise.Footprint;
import com.ben.csp.zone.BuildStage;
import com.ben.csp.zone.ConstructionSite;
import com.ben.csp.zone.EnterpriseLifecycle;
import com.ben.csp.zone.PrombazaTerritory;
import com.ben.csp.zone.ZoneEnterprises;

import java.util.Objects;

/**
 * Turns the live zone into a plan for the генплан: the prombaza territory, the
 * roads and rail reserved on it, and every enterprise facility drawn at the stage
 * it has reached — a bare plot, a part-built shell, or a finished building. The
 * board is a picture of the site as it stands, and it fills in as the site is
 * built.
 */
public final class ZonePlanSource {

    private static final int BOUNDARY = 0xFF1A1A1A;
    private static final int RESERVED = 0xFF7A7A7A;
    private static final int STAKED = 0xFFB8A24A;
    private static final int BUILDING = 0xFF333333;
    private static final int LABEL = 0xFF404040;

    private final PrombazaTerritory territory;
    private final ZoneEnterprises zone;

    public ZonePlanSource(PrombazaTerritory territory, ZoneEnterprises zone) {
        this.territory = Objects.requireNonNull(territory, "territory");
        this.zone = Objects.requireNonNull(zone, "zone");
    }

    /** Build the plan for the whole zone, or null if there is nothing to draw. */
    public PlanDocument build() {
        if (territory == null) {
            return null;
        }
        PlanDocument doc = new PlanDocument("План промбазы");

        PlanLayer boundary = doc.layer("boundary");
        Footprint b = territory.boundary();
        boundary.add(new PlanPrimitive.Polyline(
            new double[] {b.minX(), b.maxX(), b.maxX(), b.minX()},
            new double[] {b.minZ(), b.minZ(), b.maxZ(), b.maxZ()},
            true, PlanStyle.of(BOUNDARY, 2.0)));

        PlanLayer reserved = doc.layer("reserved");
        for (Footprint r : territory.reserved()) {
            reserved.add(new PlanPrimitive.Rect(r.minX(), r.minZ(),
                r.width(), r.height(), false, PlanStyle.of(RESERVED, 1.0)));
        }

        PlanLayer sites = doc.layer("facilities");
        PlanLayer labels = doc.layer("labels");
        if (zone != null) {
            for (EnterpriseLifecycle l : zone.lifecycles()) {
                for (Facility f : l.enterprise().facilities()) {
                    if (!f.hasFootprint()) {
                        continue;
                    }
                    Footprint p = f.footprint();
                    BuildStage stage = stageOf(l, f);
                    boolean building = f.isBuilt() || stage == BuildStage.FINISHED;
                    sites.add(new PlanPrimitive.Rect(p.minX(), p.minZ(), p.width(), p.height(),
                        building, PlanStyle.of(building ? BUILDING : STAKED,
                            building ? 1.5 : 1.0)));
                    labels.add(new PlanPrimitive.Label(p.centerX(), p.centerZ(),
                        l.enterprise().name(), PlanStyle.of(LABEL)));
                }
            }
        }
        return doc;
    }

    private static BuildStage stageOf(EnterpriseLifecycle l, Facility f) {
        for (ConstructionSite s : l.sites()) {
            if (s.facilityId().equals(f.id())) {
                return s.stage();
            }
        }
        return f.isBuilt() ? BuildStage.FINISHED : BuildStage.STAKED;
    }
}
