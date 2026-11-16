package com.ben.csp.prombaza;

import com.ben.csp.logistics.Defect;
import com.ben.csp.logistics.MaterialBatch;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/**
 * Self-test for the siding and the desk as one system: the local money pressure
 * (demurrage on wagons) is what sets how thorough the входной контроль is, so a
 * backed-up yard is exactly when junk slips into the building.
 */
public final class GoodsYardSelfTest {

    private static int checks = 0;

    private static void check(boolean ok, String what) {
        checks++;
        if (!ok) {
            throw new AssertionError(what);
        }
    }

    private static void eq(Object expected, Object actual, String what) {
        checks++;
        if (!expected.equals(actual)) {
            throw new AssertionError(what + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }

    public static void main(String[] args) {
        // --- demurrage: free time, then a fine per wagon per day ---
        GoodsYard yard = new GoodsYard("pb1");
        MaterialBatch b = new MaterialBatch("steel", 10, 1.0, "s1");
        GoodsYard.Wagon w = yard.arrive(b, 100);
        eq(0.0, w.demurrage(100), "no fine on arrival day");
        eq(0.0, w.demurrage(102), "still inside free time (2 days)");
        check(w.demurrage(103) > 0, "fine begins after free time");
        eq(GoodsYard.FINE_PER_WAGON_DAY, w.demurrage(103), "one day over -> one day's fine");
        eq(GoodsYard.FINE_PER_WAGON_DAY * 3, w.demurrage(105), "three days over -> three days' fine");

        // --- a cleared wagon stops accruing ---
        PrombazaControl desk = new PrombazaControl("pb1");
        yard.clearOldest(desk, 104, 1.0);
        eq(0, yard.standingCount(), "the wagon is off the rails");
        eq(GoodsYard.FINE_PER_WAGON_DAY * 2, w.demurrage(200),
            "a cleared wagon owes no more than it had when cleared");

        // --- the backlog sets the desk's load: many standing -> rushed check ---
        // Same defect either way; only the yard's backlog differs. A backed-up
        // siding must let more of them through than a quiet one.
        int trials = 120;
        GoodsYard busy = new GoodsYard("pb1");
        PrombazaControl busyDesk = new PrombazaControl("pb1");
        for (int i = 0; i < trials; i++) {
            busy.arrive(visibleDefect(), 0);
        }
        int missedWhenBusy = 0;
        // one inspector against the whole backlog: the check is rushed
        while (busy.standingCount() > 0) {
            var v = busy.clearOldest(busyDesk, 0, 1.0);
            if (v != null && v.latentDefect()) {
                missedWhenBusy++;
            }
        }
        int missedWhenQuiet = 0;
        // one wagon, many inspectors: the check is not rushed
        for (int i = 0; i < trials; i++) {
            GoodsYard quiet = new GoodsYard("pb1");
            quiet.arrive(visibleDefect(), 0);
            var v = quiet.clearOldest(new PrombazaControl("pb1"), 0, 10.0);
            if (v != null && v.latentDefect()) {
                missedWhenQuiet++;
            }
        }
        check(missedWhenBusy > missedWhenQuiet,
            "a backed-up siding lets more latent defects through than a quiet one"
                + " (busy=" + missedWhenBusy + ", quiet=" + missedWhenQuiet + ")");

        // --- the yard books the demurrage it is costing ---
        check(busy.demurrage(50) == 0, "everything was cleared, so no standing demurrage");

        System.out.println("GoodsYardSelfTest: " + checks + " checks passed.");
    }

    /** A batch carrying one defect that is easy enough to see for thoroughness to matter. */
    private static MaterialBatch visibleDefect() {
        MaterialBatch m = new MaterialBatch("cable", 1, 0.5, "s");
        m.getDefects().clear();
        m.getDefects().add(new Defect("HAIRLINE", 0.3, 0.9, new Vec3d(0, 0, 0)));
        return m;
    }
}
