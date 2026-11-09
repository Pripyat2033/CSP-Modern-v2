package com.ben.csp.authorisation;

import com.ben.csp.enterprise.Enterprise;
import com.ben.csp.enterprise.EnterpriseRoster;
import com.ben.csp.enterprise.Facility;
import com.ben.csp.enterprise.Footprint;
import com.ben.csp.prombaza.Area;
import com.ben.csp.prombaza.Prombaza;
import com.ben.csp.prombaza.PrombazaRegistry;

import java.util.List;

/** Standalone self-test for the gate that forbids plant construction too early. */
public final class PlantBuildAuthorisationSelfTest {
    private static int checks = 0;

    public static void main(String[] args) {
        List<Enterprise> enterprises = EnterpriseRoster.provisional();
        PrombazaRegistry prombazas = new PrombazaRegistry();

        // nothing is ready and no prombaza: blocked on both counts
        PlantBuildAuthorisation gate = new PlantBuildAuthorisation(enterprises, prombazas);
        check(!gate.isGranted(), "nothing authorised at the start");
        eq(0, gate.readyCount(), "no enterprise is ready yet");
        eq(2, gate.blockers().size(), "both blockers apply");
        check(gate.blockers().contains(PlantBuildAuthorisation.Blocker.ENTERPRISES_NOT_READY),
            "enterprises are named as a blocker");
        check(gate.blockers().contains(PlantBuildAuthorisation.Blocker.NO_PROMBAZA),
            "the missing prombaza is named as a blocker");
        expectThrow(gate::require, "plant construction is refused at the start");

        // designate a prombaza, but the enterprises are still not ready
        prombazas.designate("pb1", "Central", Area.of(0, 60, 0, 200, 80, 200), 1, "Ben");
        check(gate.prombazaDesignated(), "the prombaza now exists");
        check(!gate.isGranted(), "still blocked: the enterprises are not ready");
        eq(1, gate.blockers().size(), "only the enterprises block now");

        // stand the enterprises up
        for (Enterprise e : enterprises) {
            for (Facility f : e.facilities()) {
                f.site(Footprint.of(0, 0, 20, 20));
                f.completeConstruction();
                f.assignFacilityDirector("dir-" + f.id());
                f.setStaff(f.requiredStaff());
            }
        }
        eq(enterprises.size(), gate.readyCount(), "every enterprise is now operational");
        check(gate.enterprisesReady(), "the enterprises are ready");
        check(gate.isGranted(), "plant construction is authorised once both are true");
        gate.require(); // must not throw

        // knock one enterprise back: the gate closes again
        Enterprise first = enterprises.get(0);
        first.facilities().get(0).setStaff(0);
        check(!first.isOperational(), "an unmanned facility closes the enterprise");
        check(!gate.isGranted(), "the gate closes again if an enterprise stops working");
        expectThrow(gate::require, "plant construction is refused again");

        System.out.println("PlantBuildAuthorisationSelfTest: " + checks + " checks passed.");
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
    private static void expectThrow(Runnable r, String what) {
        checks++;
        try { r.run(); } catch (RuntimeException e) { return; }
        throw new AssertionError(what + ": expected an exception");
    }
}
