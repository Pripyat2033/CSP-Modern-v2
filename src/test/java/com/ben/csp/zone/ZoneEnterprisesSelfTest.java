package com.ben.csp.zone;

import com.ben.csp.authorisation.PlantBuildAuthorisation;
import com.ben.csp.enterprise.Enterprise;
import com.ben.csp.enterprise.EnterpriseRoster;
import com.ben.csp.enterprise.Facility;
import com.ben.csp.prombaza.Area;
import com.ben.csp.prombaza.PrombazaRegistry;

import java.util.List;

/** Standalone self-test: territory first, then the enterprises build themselves. */
public final class ZoneEnterprisesSelfTest {
    private static int checks = 0;

    public static void main(String[] args) {
        List<Enterprise> roster = EnterpriseRoster.provisional();
        eq(10, roster.size(), "the roster is ten enterprises");

        // 1. THE TERRITORY IS LAID OUT FIRST, from the plans
        Area area = Area.of(0, 60, 0, 240, 80, 240);
        PrombazaTerritory territory = PrombazaTerritory.standard("pb1", area);
        check(territory.area() > 0, "the territory has ground");
        check(territory.reserved().size() >= 2, "the roads and rail are reserved first");

        // 2. THE ENTERPRISES COME ONTO IT AND CHOOSE THEIR OWN PLOTS
        ZoneEnterprises zone = ZoneEnterprises.spawn(territory, roster, 1);
        eq(10, zone.count(), "ten enterprises are standing on the territory");
        eq(0, zone.operationalCount(), "none is operational at the start");

        PrombazaRegistry prombazas = new PrombazaRegistry();
        PlantBuildAuthorisation gate = new PlantBuildAuthorisation(zone.enterprises(), prombazas);
        check(!gate.isGranted(), "plant construction is not authorised yet");

        int newlyOperational = 0;
        int ticks = 0;
        boolean sawMarker = false;   // a facility set out but not yet built
        boolean sawGrowing = false;  // a facility part-way up
        while (!zone.allOperational() && ticks < 8000) {
            newlyOperational += zone.tick(ticks);
            for (EnterpriseLifecycle l : zone.lifecycles()) {
                for (ConstructionSite s : l.sites()) {
                    if (s.stage() == BuildStage.STAKED) sawMarker = true;
                    if (s.stage() != BuildStage.FINISHED) sawGrowing = true;
                }
            }
            ticks++;
        }

        check(zone.allOperational(), "every enterprise becomes operational on its own");
        eq(10, newlyOperational, "each enterprise became operational exactly once");
        check(sawMarker, "facilities start as a marker, not a finished building");
        check(sawGrowing, "facilities grow over time, not in one step");

        // every facility has a construction site that finished
        for (EnterpriseLifecycle l : zone.lifecycles()) {
            eq(l.enterprise().facilities().size(), l.sites().size(),
                l.enterprise().id() + " opened a site for every facility");
            for (ConstructionSite s : l.sites()) {
                check(s.isComplete(), l.enterprise().id() + "/" + s.facilityId() + " was built out");
                eq(BuildStage.FINISHED, s.stage(), s.facilityId() + " finished");
                check(s.hasBrigade(), s.facilityId() + " had a brigade");
            }
        }

        // every facility chose a plot INSIDE the territory, clear of what is reserved
        for (Enterprise e : zone.enterprises()) {
            for (Facility f : e.facilities()) {
                check(f.hasFootprint(), e.id() + "/" + f.id() + " chose a plot");
                check(territory.contains(f.footprint()),
                    e.id() + "/" + f.id() + " built inside the territory");
                check(!territory.isReserved(f.footprint()),
                    e.id() + "/" + f.id() + " did not build on the roads or rail");
                check(f.isBuilt(), e.id() + "/" + f.id() + " was built");
                check(f.isAsBuiltAccepted(), e.id() + "/" + f.id() + " passed its as-built check");
                check(f.facilityDirectorId() != null, e.id() + "/" + f.id() + " has a director");
                check(f.staff() >= f.requiredStaff(), e.id() + "/" + f.id() + " is manned");
            }
        }

        // no two facilities stand on the same ground
        check(noOverlaps(zone), "no two facilities stand on the same ground");

        // 3. NOW DESIGNATE THE PROMBAZA: the gate opens
        prombazas.designate("pb1", "Central", area, ticks, "Director");
        check(gate.isGranted(), "with the enterprises ready, plant construction is authorised");
        gate.require(); // must not throw

        System.out.println("ZoneEnterprisesSelfTest: " + checks + " checks passed.");
    }

    private static boolean noOverlaps(ZoneEnterprises zone) {
        List<Facility> all = new java.util.ArrayList<>();
        for (Enterprise e : zone.enterprises()) {
            all.addAll(e.facilities());
        }
        for (int i = 0; i < all.size(); i++) {
            for (int j = i + 1; j < all.size(); j++) {
                if (PrombazaTerritory.overlaps(all.get(i).footprint(), all.get(j).footprint())) {
                    return false;
                }
            }
        }
        return true;
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
