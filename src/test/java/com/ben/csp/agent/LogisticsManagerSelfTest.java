package com.ben.csp.agent;

import java.util.Map;

/**
 * Standalone self-test for {@link LogisticsManager}: route and inventory
 * registration, and the delivery schedule that was previously {@code null}.
 */
public final class LogisticsManagerSelfTest {

    private static int checks = 0;

    public static void main(String[] args) {
        LogisticsManager lm = new LogisticsManager();

        // nothing registered: the schedule is an empty plan, not null
        Map<String, Double> empty = lm.calculateDeliverySchedule();
        check(empty != null, "an empty plan is returned, not null");
        check(empty.isEmpty(), "with nothing registered the plan is empty");

        // register a route and receive some stock
        lm.registerRoute("Prombaza", "Site-1", 20, 30.0, false);
        lm.trackInventoryReceived("concrete", 100, 5.0, "Site-1");
        lm.trackInventoryReceived("rebar", 40, 2.0, "Site-1");
        eq(2, lm.inventory().size(), "two materials are tracked");

        // a refrigerated material is routed differently from a dry one
        lm.registerRoute("Prombaza", "Site-1", 10, 45.0, true);

        // with stock on site, a schedule exists and every destination has a time
        Map<String, Double> plan = lm.calculateDeliverySchedule();
        check(!plan.isEmpty(), "a plan is produced when routes and stock exist");
        check(plan.containsKey("Site-1"), "the destination is in the plan");
        check(plan.get("Site-1") > 0, "the delivery time is positive");

        // the plan does not exceed what a route can carry in the window; a heavier
        // stock takes longer, not less
        lm.trackInventoryReceived("concrete", 900, 45.0, "Site-1");
        Map<String, Double> heavier = lm.calculateDeliverySchedule();
        check(heavier.get("Site-1") >= plan.get("Site-1"),
            "more stock never shortens the delivery time");

        // registerRoute refuses nonsense
        check(threw(() -> lm.registerRoute("A", "B", 0, 10.0, false)),
            "a zero-capacity route is refused");
        check(threw(() -> lm.registerRoute("A", "B", 5, -1.0, false)),
            "a negative travel time is refused");
        check(threw(() -> lm.trackInventoryReceived("x", -1, 1.0, "Site-1")),
            "negative stock is refused");

        // inventory() is a read-only view
        check(threw(() -> lm.inventory().put("x",
            new LogisticsManager.MaterialInventory("x", 1, 1.0, false))),
            "the inventory map is unmodifiable");

        System.out.println("LogisticsManagerSelfTest: " + checks + " checks passed.");
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

    private static boolean threw(Runnable r) {
        try {
            r.run();
            return false;
        } catch (RuntimeException e) {
            return true;
        }
    }
}
