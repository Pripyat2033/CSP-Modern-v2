package com.ben.csp.zone;

import com.ben.csp.enterprise.Enterprise;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The enterprises on a prombaza territory, stood up from the plans and then left
 * to manage themselves on the work clock. The territory comes first; the
 * enterprises come onto it, each choosing where inside it to build.
 */
public final class ZoneEnterprises {

    private final String prombazaId;
    private final List<EnterpriseLifecycle> lifecycles = new ArrayList<>();

    private ZoneEnterprises(String prombazaId) {
        this.prombazaId = prombazaId;
    }

    /**
     * Put the roster onto a laid-out territory. Every enterprise shares the same
     * ground and picks its own plots within it.
     */
    public static ZoneEnterprises spawn(PrombazaTerritory territory,
                                        List<Enterprise> roster, long day) {
        Objects.requireNonNull(territory, "territory");
        Objects.requireNonNull(roster, "roster");
        SiteAllocator allocator = new SiteAllocator(territory);
        ZoneEnterprises zone = new ZoneEnterprises(territory.id());
        for (Enterprise e : roster) {
            zone.lifecycles.add(new EnterpriseLifecycle(
                e, allocator, EnterpriseLifecycle.staffTargetFor(e)));
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
            try {
                if (l.tick(day)) {
                    newly++;
                }
            } catch (RuntimeException e) {
                // one enterprise must not stop the whole zone for the day
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
