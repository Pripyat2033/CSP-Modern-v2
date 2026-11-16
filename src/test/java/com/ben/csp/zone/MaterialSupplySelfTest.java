package com.ben.csp.zone;

import com.ben.csp.enterprise.Footprint;
import com.ben.csp.prombaza.GoodsYard;
import com.ben.csp.prombaza.PrombazaControl;

import java.util.List;

/**
 * Self-test for the supply seam: the site does not get a clean number of material.
 * It gets what the suppliers sent and the desk cleared, and that is often less
 * than it asked for — the plan and the reality are not the same thing.
 */
public final class MaterialSupplySelfTest {

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
        ConstructionSite site = new ConstructionSite(
            "cs-1", "f-1", Footprint.of(0, 0, 20, 20), 1000);

        // --- a delivery from poor suppliers does not fully arrive ---
        GoodsYard yard = new GoodsYard("pb1");
        PrombazaControl desk = new PrombazaControl("pb1");
        MaterialSupply supply = new MaterialSupply(yard, desk, List.of(
            new MaterialSupply.Supplier("artel-V", 0.5)));
        MaterialSupply.Delivery d = supply.deliverTo(site, 200, 1, 1.0);
        check(d.delivered() <= d.requested(), "you never get more than you asked for");
        check(d.lost() >= 0, "the shortfall is not negative");
        eq(d.requested(), 200.0, "the request is recorded as asked");
        check(d.fulfilment() <= 1.0, "fulfilment never exceeds 1");

        // --- good suppliers fulfil better than poor ones ---
        double goodFulfil = averageFulfilment(0.98, 40);
        double poorFulfil = averageFulfilment(0.45, 40);
        check(goodFulfil > poorFulfil,
            "good suppliers deliver more than poor ones (good=" + goodFulfil
                + ", poor=" + poorFulfil + ")");
        check(poorFulfil < 1.0, "a poor supplier does leave the site short");

        // --- the site only ever receives what the desk cleared ---
        GoodsYard yard2 = new GoodsYard("pb1");
        PrombazaControl desk2 = new PrombazaControl("pb1");
        MaterialSupply supply2 = new MaterialSupply(yard2, desk2);
        ConstructionSite site2 = new ConstructionSite("cs-2", "f-2", Footprint.of(0, 0, 20, 20), 1000);
        supply2.deliverTo(site2, 100, 1, 1.0);
        check(site2.materialsOnSite() <= 100, "the site holds no more than was delivered");
        check(desk2.receipts().size() > 0, "the desk recorded the delivery it checked");

        // --- the wagons were all cleared off the rails ---
        eq(0, yard2.standingCount(), "the yard is not left holding cleared wagons");

        // --- a request of zero or less is refused ---
        try {
            supply2.deliverTo(site2, 0, 1, 1.0);
            check(false, "zero request must throw");
        } catch (IllegalArgumentException e) {
            check(true, "zero request throws");
        }

        System.out.println("MaterialSupplySelfTest: " + checks + " checks passed.");
    }

    private static double averageFulfilment(double supplierQuality, int trials) {
        double sum = 0;
        for (int i = 0; i < trials; i++) {
            GoodsYard yard = new GoodsYard("pb1");
            PrombazaControl desk = new PrombazaControl("pb1");
            MaterialSupply supply = new MaterialSupply(yard, desk, List.of(
                new MaterialSupply.Supplier("s", supplierQuality)));
            ConstructionSite site = new ConstructionSite(
                "cs", "f", Footprint.of(0, 0, 20, 20), 1000);
            sum += supply.deliverTo(site, 100, 1, 1.0).fulfilment();
        }
        return sum / trials;
    }
}
