package com.ben.csp.zone;

import com.ben.csp.enterprise.Enterprise;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The enterprises of the zone, stood up from a plan and then left to manage
 * themselves on the work clock. This is the piece that turns a roster into a
 * working site: spawn them, tick the clock, and watch them build themselves out.
 */
public final class ZoneEnterprises {

    private final String prombazaId;
    private final List<EnterpriseLifecycle> lifecycles = new ArrayList<>();

    private ZoneEnterprises(String prombazaId) {
        this.prombazaId = prombazaId;
    }

    /** Stand up every enterprise in the roster at the sites the plan gives it. */
    public static ZoneEnterprises spawn(ZonePlan plan, List<Enterprise> roster, long day) {
        Objects.requireNonNull(plan, "plan");
        Objects.requireNonNull(roster, "roster");
        ZoneEnterprises zone = new ZoneEnterprises(plan.prombazaId());
        for (Enterprise e : roster) {
            List<com.ben.csp.enterprise.Footprint> sites = plan.sitesFor(e.id());
            if (sites == null) {
                throw new IllegalStateException("the plan has no site for " + e.id());
            }
            zone.lifecycles.add(new EnterpriseLifecycle(e, sites, plan.staffTargetFor(e.id())));
        }
        return zone;
    }

    public String prombazaId() { return prombazaId; }
    public int count() { return lifecycles.size(); }

    public List<EnterpriseLifecycle> lifecycles() {
        return Collections.unmodifiableList(lifecycles);
    }

    public List<Enterprise> enterprises() {
        List<Enterprise> out = new ArrayList<>();
        for (EnterpriseLifecycle l : lifecycles) {
            out.add(l.enterprise());
        }
        return out;
    }

    /** Advance every enterprise one step. Returns how many just became operational. */
    public int tick(long day) {
        int newly = 0;
        for (EnterpriseLifecycle l : lifecycles) {
            if (l.tick(day)) {
                newly++;
            }
        }
        return newly;
    }

    public int operationalCount() {
        return (int) lifecycles.stream().filter(l -> l.enterprise().isOperational()).count();
    }

    public boolean allOperational() {
        return !lifecycles.isEmpty()
            && lifecycles.stream().allMatch(l -> l.enterprise().isOperational());
    }

    @Override
    public String toString() {
        return "zone[" + prombazaId + "] " + operationalCount() + "/" + count()
            + " enterprises operational";
    }
}
