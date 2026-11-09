package com.ben.csp.zone;

import com.ben.csp.authorisation.PlantBuildAuthorisation;
import com.ben.csp.enterprise.Enterprise;
import com.ben.csp.enterprise.EnterpriseRoster;
import com.ben.csp.enterprise.Facility;
import com.ben.csp.prombaza.Area;
import com.ben.csp.prombaza.PrombazaRegistry;

import java.util.List;

/** Standalone self-test: the enterprises spawn and manage themselves to ready. */
public final class ZoneEnterprisesSelfTest {
    private static int checks = 0;

    public static void main(String[] args) {
        List<Enterprise> roster = EnterpriseRoster.provisional();
        eq(10, roster.size(), "the roster is ten enterprises");

        // lay the zone out and spawn the enterprises into it
        ZonePlan plan = ZonePlan.standard("pb1", 0, 0, roster);
        eq(10, plan.size(), "the plan gives every enterprise ground");
        ZoneEnterprises zone = ZoneEnterprises.spawn(plan, roster, 1);

        eq(10, zone.count(), "ten enterprises are standing");
        eq(0, zone.operationalCount(), "none is operational at the start");
        check(!zone.allOperational(), "the zone is not ready at the start");

        // the plant cannot be built yet
        PrombazaRegistry prombazas = new PrombazaRegistry();
        PlantBuildAuthorisation gate = new PlantBuildAuthorisation(zone.enterprises(), prombazas);
        check(!gate.isGranted(), "plant construction is not authorised yet");

        // tick the work clock; the enterprises manage themselves
        int newlyOperational = 0;
        int ticks = 0;
        while (!zone.allOperational() && ticks < 2000) {
            newlyOperational += zone.tick(ticks);
            ticks++;
        }

        check(zone.allOperational(), "every enterprise becomes operational on its own");
        eq(10, zone.operationalCount(), "all ten are operational");
        eq(10, newlyOperational, "each enterprise became operational exactly once");
        check(ticks < 2000, "the zone stood itself up in a sane number of ticks");

        // every facility was sited, built, checked and manned
        for (Enterprise e : zone.enterprises()) {
            for (Facility f : e.facilities()) {
                check(f.hasFootprint(), e.id() + "/" + f.id() + " was set out");
                check(f.isBuilt(), e.id() + "/" + f.id() + " was built");
                check(f.isAsBuiltAccepted(), e.id() + "/" + f.id() + " passed its as-built check");
                check(f.facilityDirectorId() != null, e.id() + "/" + f.id() + " has a director");
                check(f.staff() >= f.requiredStaff(), e.id() + "/" + f.id() + " is manned");
            }
        }

        // now that the enterprises work, designate a prombaza: the gate opens
        prombazas.designate("pb1", "Central", Area.of(0, 60, 0, 200, 80, 200), ticks, "Director");
        check(gate.prombazaDesignated(), "a prombaza is designated");
        check(gate.isGranted(), "with the enterprises ready, plant construction is authorised");
        gate.require(); // must not throw

        System.out.println("ZoneEnterprisesSelfTest: " + checks + " checks passed.");
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
