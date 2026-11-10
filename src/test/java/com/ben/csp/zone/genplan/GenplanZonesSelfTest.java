package com.ben.csp.zone.genplan;

import com.ben.csp.enterprise.Enterprise;
import com.ben.csp.enterprise.EnterpriseRoster;
import com.ben.csp.enterprise.Footprint;
import com.ben.csp.infra.RouteKind;

import java.util.List;

/** Standalone self-test for the zoned general plan (СНиП II-89-80*). */
public final class GenplanZonesSelfTest {

    private static int checks = 0;

    public static void main(String[] args) {
        List<Enterprise> roster = EnterpriseRoster.provisional();
        Footprint site = Footprint.of(0, 0, 260, 260);
        GenplanZones g = GenplanZones.layOut(site, roster);

        // every facility of every enterprise is placed on a 260x260 site
        int want = 0, placed = 0;
        for (Enterprise e : roster) {
            for (int i = 0; i < e.facilities().size(); i++) {
                want++;
                if (g.plot(e.id(), i) != null) {
                    placed++;
                }
            }
        }
        eq(want, placed, "every facility is placed on a 260x260 site");

        // the functional zones hold the right trades
        var zc = g.zoneCounts();
        eq(7, zc.getOrDefault(Zone.PRODUCTION, 0), "production zone holds the plants and workshops");
        eq(5, zc.getOrDefault(Zone.STORAGE, 0), "storage zone holds the yards and stores");
        eq(2, zc.getOrDefault(Zone.AUXILIARY, 0), "auxiliary zone holds the depots");
        check(zc.getOrDefault(Zone.PREDZAVODSKAYA, 0) >= 10, "the pre-plant zone holds the offices");

        // every plot lies inside the site boundary
        java.util.List<Footprint> all = new java.util.ArrayList<>();
        for (Enterprise e : roster) {
            for (int i = 0; i < e.facilities().size(); i++) {
                Footprint f = g.plot(e.id(), i);
                if (f == null) {
                    continue;
                }
                all.add(f);
                check(f.minX() >= site.minX() && f.maxX() <= site.maxX()
                    && f.minZ() >= site.minZ() && f.maxZ() <= site.maxZ(),
                    "plot inside the boundary: " + f);
            }
        }

        // no two plots overlap: the site is a plan, not a heap
        for (int i = 0; i < all.size(); i++) {
            for (int j = i + 1; j < all.size(); j++) {
                check(!overlaps(all.get(i), all.get(j)), "no two plots overlap");
            }
        }

        // the rail spur runs along the far edge; the roads are laid
        check(g.routes().stream().anyMatch(r -> r.kind() == RouteKind.RAIL),
            "the rail spur is laid");
        check(g.routes().stream().filter(r -> r.kind() == RouteKind.ROAD).count() >= 2,
            "the roads are laid");

        // the plan is deterministic: the same ground yields the same site
        GenplanZones again = GenplanZones.layOut(site, roster);
        eq(g.hqPlot("genstroi").toString(), again.hqPlot("genstroi").toString(),
            "the plan is deterministic");
        eq(g.density(), again.density(), "the density is deterministic");

        // a narrow 237x51 strip still gets the pre-plant zone (offices) laid out
        GenplanZones narrow = GenplanZones.layOut(Footprint.of(0, 0, 237, 51), roster);
        check(narrow.zoneCounts().getOrDefault(Zone.PREDZAVODSKAYA, 0) >= 7,
            "a narrow strip still lays the offices out");

        // the norm requires a minimum density, and the plan reports against it
        eq(0.50, g.densityRequired(), "the norm's minimum density is 50 %");

        System.out.println("GenplanZonesSelfTest: " + checks + " checks passed.");
    }

    private static boolean overlaps(Footprint a, Footprint b) {
        return a.minX() < b.maxX() && b.minX() < a.maxX()
            && a.minZ() < b.maxZ() && b.minZ() < a.maxZ();
    }

    private static void eq(Object e, Object a, String what) {
        checks++;
        if (!String.valueOf(e).equals(String.valueOf(a))) {
            throw new AssertionError(what + ": expected <" + e + "> but was <" + a + ">");
        }
    }

    private static void check(boolean cond, String what) {
        checks++;
        if (!cond) {
            throw new AssertionError(what);
        }
    }
}
