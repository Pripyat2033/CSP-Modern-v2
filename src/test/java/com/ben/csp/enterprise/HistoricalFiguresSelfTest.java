package com.ben.csp.enterprise;

/** Standalone self-test for the recorded historical people of the construction. */
public final class HistoricalFiguresSelfTest {
    private static int checks = 0;

    public static void main(String[] args) {
        eq(3, HistoricalFigures.all().size(), "three people are recorded");

        HistoricalPerson kizima = HistoricalFigures.KIZIMA;
        check(kizima.fullName().contains("Кизима"), "Kizima is recorded by name");
        check(kizima.role().toLowerCase().contains("construction"),
            "Kizima headed the construction administration");
        eq(Provenance.VERIFIED, kizima.provenance(), "Kizima is a cited fact");
        check(kizima.source().contains("wikipedia"), "Kizima's source is cited");

        HistoricalPerson bryukhanov = HistoricalFigures.BRYUKHANOV;
        check(bryukhanov.role().toLowerCase().contains("director"),
            "Bryukhanov was the director");
        eq(Provenance.VERIFIED, bryukhanov.provenance(), "Bryukhanov is a cited fact");

        HistoricalPerson protsenko = HistoricalFigures.PROTSENKO;
        check(protsenko.role().toLowerCase().contains("architect"),
            "Protsenko was the architect of Pripyat");

        eq(kizima, HistoricalFigures.byId("kizima"), "a person can be found by id");
        check(HistoricalFigures.byId("nobody") == null, "an unknown id yields nobody");

        System.out.println("HistoricalFiguresSelfTest: " + checks + " checks passed.");
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
