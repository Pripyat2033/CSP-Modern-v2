package com.ben.csp.prombaza;

/** Standalone self-test for the prombaza domain. */
public final class PrombazaSelfTest {
    private static int checks = 0;

    public static void main(String[] args) {
        // Area normalises corners given in any order
        Area a = Area.of(10, 60, -5, 0, 70, 5);
        eq(0, a.minX(), "min corner normalised");
        eq(10, a.maxX(), "max corner normalised");
        eq(-5, a.minZ(), "min z normalised");
        check(a.contains(5, 65, 0), "point inside");
        check(!a.contains(11, 65, 0), "point outside x");
        eq(11L * 11 * 11, a.volume(), "volume counts blocks inclusively");

        PrombazaRegistry reg = new PrombazaRegistry();
        check(reg.isEmpty(), "registry starts empty");

        Prombaza p = reg.designate("pb-1", "Central Prombaza", a, 1, "Ben");
        eq(1, reg.size(), "one prombaza designated");
        eq("Central Prombaza", p.name(), "name kept");
        check(reg.at(5, 65, 0).isPresent(), "lookup by coordinate finds it");
        check(reg.byId("pb-1").isPresent(), "lookup by id finds it");

        // duplicate id refused
        expectThrow(() -> reg.designate("pb-1", "Other", Area.of(50, 60, 50, 60, 70, 60), 2, "Ben"),
            "duplicate id refused");

        // overlapping area refused (protects against a mis-drawn designation)
        expectThrow(() -> reg.designate("pb-2", "Overlap", Area.of(5, 60, 0, 20, 70, 10), 2, "Ben"),
            "overlapping area refused");

        // a disjoint area is fine
        Prombaza q = reg.designate("pb-2", "North Yard", Area.of(100, 60, 100, 120, 70, 120), 2, "Ben");
        eq(2, reg.size(), "second prombaza designated");

        // undo
        check(reg.undo("pb-2"), "undo removes it");
        eq(1, reg.size(), "back to one prombaza");
        check(!reg.undo("pb-2"), "undo of a missing id returns false");
        check(reg.byId("pb-1").isPresent(), "the original is untouched");

        // re-designate after undo (id freed)
        reg.designate("pb-2", "North Yard", Area.of(100, 60, 100, 120, 70, 120), 3, "Ben");
        eq(2, reg.size(), "re-designated after undo");

        // guards
        check(Area.of(5, 5, 5, 1, 1, 1).minX() == 1, "of() normalises reversed corners");
        expectThrow(() -> new Area(5, 5, 5, 1, 6, 6), "min>max rejected");

        System.out.println("PrombazaSelfTest: " + checks + " checks passed.");
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
