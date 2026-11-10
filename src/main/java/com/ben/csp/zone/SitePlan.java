package com.ben.csp.zone;

import com.ben.csp.enterprise.Enterprise;
import com.ben.csp.enterprise.Footprint;
import com.ben.csp.infra.Route;
import com.ben.csp.zone.genplan.GenplanZones;
import com.ben.csp.zone.genplan.Zone;

import java.util.List;
import java.util.Map;

/**
 * The general plan (генплан) the enterprises agree for a prombaza: which zone each
 * building stands in, where the rail spur comes in, and where the roads run. It is
 * worked out to СНиП II-89-80* by {@link GenplanZones} and is deterministic, so the
 * same ground always yields the same site and a saved zone rebuilds itself without
 * the plan stored.
 */
public final class SitePlan {

    private final GenplanZones genplan;

    private SitePlan(GenplanZones genplan) {
        this.genplan = genplan;
    }

    /** The plot of the facility at this index in the enterprise's own order. */
    public Footprint plot(String enterpriseId, int index) {
        return genplan.plot(enterpriseId, index);
    }

    /** The plot of the enterprise's office. */
    public Footprint hqPlot(String enterpriseId) {
        return genplan.hqPlot(enterpriseId);
    }

    public List<Route> routes() {
        return genplan.routes();
    }

    /** How many facilities stand in each functional zone. */
    public Map<Zone, Integer> zoneCounts() {
        return genplan.zoneCounts();
    }

    /** The share of the site built on, and the share СНиП II-89-80* requires. */
    public double density() { return genplan.density(); }
    public double densityRequired() { return genplan.densityRequired(); }

    /**
     * Survey the territory and lay the roster out on it by function. An enterprise
     * that cannot be given ground is simply left out of the plan: it will wait for
     * more ground rather than spoil the site.
     */
    public static SitePlan layOut(PrombazaTerritory territory, List<Enterprise> roster) {
        return new SitePlan(GenplanZones.layOut(territory.boundary(), roster));
    }
}
