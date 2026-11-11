package com.ben.csp.plan;

import com.ben.csp.enterprise.Enterprise;
import com.ben.csp.enterprise.EnterpriseRoster;
import com.ben.csp.prombaza.Area;
import com.ben.csp.zone.PrombazaTerritory;
import com.ben.csp.zone.ZoneEnterprises;

import java.util.List;

/** Standalone self-test: the генплан draws the live zone, and fills in as it builds. */
public final class ZonePlanSourceSelfTest {
    private static int checks = 0;

    public static void main(String[] args) {
        Area area = Area.of(0, 90, 0, 360, 120, 360);
        PrombazaTerritory territory = PrombazaTerritory.standard("pb1", area);
        List<Enterprise> roster = EnterpriseRoster.provisional();
        ZoneEnterprises zone = ZoneEnterprises.spawn(territory, roster, 0);
        ZonePlanSource source = new ZonePlanSource(territory, zone);

        // an empty zone: the boundary, and the ten offices already staked out. The
        // enterprises site their offices the moment they survey the ground, so those
        // stand on the board before any building work starts; the rest fills in later.
        PlanDocument empty = source.build();
        check(empty != null, "the board draws something");
        eq(1, count(empty, "boundary"), "the territory boundary is drawn");
        eq(territory.reserved().size(), count(empty, "reserved"),
            "the reserved roads and rail are drawn");
        eq(10, count(empty, "facilities"),
            "the ten offices are staked out before the building work starts");

        // run the zone and watch the board fill in
        int firstPlots = -1;
        int t = 0;
        while (!zone.allOperational() && t < 9000) {
            zone.tick(t++);
        }
        PlanDocument full = source.build();
        int plots = count(full, "facilities");
        check(plots > 0, "the facilities appear on the board");
        check(plots > count(empty, "facilities"), "the board fills in as the site is built");
        eq(count(full, "labels"), plots, "every facility is labelled with its enterprise");

        // the board is bounded by the territory
        for (PlanPrimitive p : full.layer("facilities").primitives()) {
            PlanPrimitive.Rect r = (PlanPrimitive.Rect) p;
            check(r.x() >= area.minX() && r.x() + r.w() <= area.maxX(),
                "a facility is drawn inside the territory (x)");
            check(r.z() >= area.minZ() && r.z() + r.h() <= area.maxZ(),
                "a facility is drawn inside the territory (z)");
        }

        System.out.println("ZonePlanSourceSelfTest: " + checks + " checks passed.");
    }

    private static int count(PlanDocument doc, String layer) {
        return doc.layer(layer).primitives().size();
    }

    private static void eq(Object e, Object a, String what) {
        checks++;
        if (!String.valueOf(e).equals(String.valueOf(a))) {
            throw new AssertionError(what + ": expected <" + e + "> but was <" + a + ">");
        }
    }
    private static void check(boolean cond, String what) {
        checks++;
        if (!cond) throw new AssertionError(what);
    }
}
