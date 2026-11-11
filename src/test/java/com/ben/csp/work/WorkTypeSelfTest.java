package com.ben.csp.work;

/**
 * Standalone self-test for {@link WorkType}: every norm must declare where it
 * came from, and a provisional figure must never be able to pass itself off as a
 * sourced one. This is the guard the "placeholder" comment could not give.
 */
public final class WorkTypeSelfTest {

    private static int checks = 0;

    public static void main(String[] args) {
        // every trade states its basis and its source category
        for (WorkType t : WorkType.values()) {
            check(t.basis() != null && !t.basis().isBlank(),
                t + " states the basis of its norm");
            check(t.source() != null, t + " has a source category");
        }

        // the lump-sum trade is "без нормы" by definition — not a placeholder
        eq(NormSource.NOT_APPLICABLE, WorkType.LUMP.source(),
            "lump work is без нормы, not provisional");
        check(!WorkType.LUMP.isProvisional(), "lump work is not a placeholder");
        eq(0.0, WorkType.LUMP.manHoursPerUnit(), "lump work carries no per-unit norm");

        // a provisional norm is positive, says so, and names the collection it waits on
        for (WorkType t : WorkType.values()) {
            if (t.source() == NormSource.PROVISIONAL) {
                check(t.manHoursPerUnit() > 0, t + " has a positive provisional norm");
                check(t.isProvisional(), t + " reports provisional");
                check(!t.isSourced(), t + " does not claim to be sourced");
                check(t.pendingCollection() != null && !t.pendingCollection().isBlank(),
                    t + " names the ЕНиР collection its figure must come from");
            }
        }

        // the trades exist and are measured in the physical units Soviet norms used
        eq(WorkUnit.CUBIC_METRE, WorkType.EXCAVATION.unit(), "excavation is per m³");
        eq(WorkUnit.TONNE, WorkType.REBAR_INSTALLATION.unit(), "rebar is per tonne");
        eq(WorkUnit.LINEAR_METRE, WorkType.CABLE_PULLING.unit(), "cabling is per linear metre");
        eq(WorkUnit.POINT, WorkType.GEODETIC_SURVEY.unit(), "survey is per point");

        // the наряд math still holds: quantity x norm = man-hours
        eq(60.0, WorkType.EXCAVATION.manHoursPerUnit() * 100.0, "100 m³ of digging at the norm");

        System.out.println("WorkTypeSelfTest: " + checks + " checks passed.");
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
