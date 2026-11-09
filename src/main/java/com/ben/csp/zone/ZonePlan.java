package com.ben.csp.zone;

import com.ben.csp.enterprise.Enterprise;
import com.ben.csp.enterprise.Facility;
import com.ben.csp.enterprise.Footprint;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The layout of the zone: where each enterprise and each of its facilities is to
 * stand, and how many staff each enterprise needs. The plan gives the enterprises
 * their ground; from it they are stood up and can manage themselves.
 */
@Deprecated
// TODO: User to delete this file later. Superseded: the prombaza territory is laid
// out first (PrombazaTerritory), and each enterprise CHOOSES its own plots inside
// it (SiteAllocator); enterprises are not handed fixed sites.
public final class ZonePlan {

    private final String prombazaId;
    private final Map<String, List<Footprint>> sites = new LinkedHashMap<>();
    private final Map<String, Integer> staffTargets = new LinkedHashMap<>();

    public ZonePlan(String prombazaId) {
        this.prombazaId = Objects.requireNonNull(prombazaId, "prombazaId");
    }

    public ZonePlan place(String enterpriseId, List<Footprint> footprints, int staffTarget) {
        sites.put(enterpriseId, List.copyOf(footprints));
        staffTargets.put(enterpriseId, staffTarget);
        return this;
    }

    public String prombazaId() { return prombazaId; }
    public List<Footprint> sitesFor(String enterpriseId) { return sites.get(enterpriseId); }
    public int staffTargetFor(String enterpriseId) { return staffTargets.getOrDefault(enterpriseId, 0); }
    public Set<String> enterpriseIds() { return sites.keySet(); }
    public int size() { return sites.size(); }

    /**
     * Lay the whole roster out around a centre: one block per enterprise, one
     * plot per facility. Deterministic, so the zone is the same every time.
     */
    public static ZonePlan standard(String prombazaId, int centerX, int centerZ,
                                    List<Enterprise> roster) {
        ZonePlan plan = new ZonePlan(prombazaId);
        int i = 0;
        for (Enterprise e : roster) {
            int blockX = centerX + (i % 4) * 96;
            int blockZ = centerZ + (i / 4) * 96;
            List<Footprint> plots = new ArrayList<>();
            int j = 0;
            for (Facility ignored : e.facilities()) {
                int x = blockX + (j % 2) * 40;
                int z = blockZ + (j / 2) * 40;
                plots.add(Footprint.of(x, z, x + 24, z + 24));
                j++;
            }
            int staff = e.facilities().stream().mapToInt(Facility::requiredStaff).sum() + 8;
            plan.place(e.id(), plots, staff);
            i++;
        }
        return plan;
    }
}
