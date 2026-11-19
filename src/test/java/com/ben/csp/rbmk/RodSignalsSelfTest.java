package com.ben.csp.rbmk;

import com.ben.csp.rbmk.plant.Instrumentation;

/**
 * Proves the control-rod signal family is complete and regulation-sourced.
 *
 * <p>Ben challenged the old "211 control channels × 1 signal". The regulation
 * (§2.11.6) says the 211 rods are seven types, and each rod carries more than a
 * position. This test holds the model to that.
 */
public final class RodSignalsSelfTest {

    private static int checks = 0;
    private static int failures = 0;

    public static void main(String[] args) {
        System.out.println("=== RodSignalsSelfTest ===");

        // 1. The seven rod types sum to exactly the second-generation control count.
        check("the seven rod types sum to 211 (regulation §2.11.6)",
            RodType.total() == 211, "total=" + RodType.total());
        check("АЗ = 24, ПК-АЗ = 24, ПК-РР = 12, РР = 103 (verbatim counts)",
            RodType.AZ.count == 24 && RodType.PK_AZ.count == 24
                && RodType.PK_RR.count == 12 && RodType.RR.count == 103,
            "AZ=24 PK-AZ=24 PK-RR=12 RR=103");

        // 2. Overcompensation (ПК) exists for the АЗ and РР rods.
        check("overcompensation (ПК) is carried by the ПК-АЗ and ПК-РР rods only",
            RodType.PK_AZ.hasOvercompensation() && RodType.PK_RR.hasOvercompensation()
                && !RodType.AZ.hasOvercompensation() && !RodType.RR.hasOvercompensation(),
            "ПК-АЗ + ПК-РР");

        // 3. The cartogram has one entry per control channel.
        check("cartogram has one rod type per control channel (211)",
            RodType.cartogram(211).size() == 211, "size=" + RodType.cartogram(211).size());

        // 4. The signal list carries the full per-rod family.
        var signals = Instrumentation.all();
        long pos = count(signals, Instrumentation.Kind.ROD_POSITION);
        long nk = count(signals, Instrumentation.Kind.ROD_LIMIT_LOWER);
        long vk = count(signals, Instrumentation.Kind.ROD_LIMIT_UPPER);
        long pk = count(signals, Instrumentation.Kind.ROD_OVERCOMP);
        long lampDepth = count(signals, Instrumentation.Kind.ROD_LAMP_DEPTH);
        long lampTop = count(signals, Instrumentation.Kind.ROD_LAMP_TOP);

        check("every rod has a position signal (211)",
            pos == 211, "pos=" + pos);
        check("every rod has a lower limit switch НК (211)",
            nk == 211, "НК=" + nk);
        check("every rod has an upper limit switch ВК (211)",
            vk == 211, "ВК=" + vk);
        // Overcompensation (ПК) exists on 24 ПК-АЗ + 12 ПК-РР rods. The panel drawing
        // localises the ПК-АЗ (its own grey colour) but draws all 127 manual rods in
        // ONE blue category, so it cannot say WHICH 12 of them are ПК-РР. Rather than
        // guess a subset, the cartogram reports the 24 it can place and the model
        // carries the 12 ПК-РР as a known sub-population with an unresolved position.
        check("overcompensation ПК signal on the 24 placeable ПК-АЗ rods "
                + "(the 12 ПК-РР are inside the panel's single blue category)",
            pk == 24, "ПК=" + pk + " (ПК-АЗ 24 localised; ПК-РР 12 not resolvable)");
        check("every rod has the green 'full depth' selsyn lamp (211)",
            lampDepth == 211, "lamp_depth=" + lampDepth);
        check("every rod has the orange 'top of stops' selsyn lamp (211)",
            lampTop == 211, "lamp_top=" + lampTop);

        // 5. The rod family is now many signals per rod, not one.
        long rodTotal = pos + nk + vk + pk + lampDepth + lampTop;
        check("the control-rod family is far more than 211 signals (was 211 × 1)",
            rodTotal == 211 * 5 + 24, "rod family total=" + rodTotal);

        // 6. The whole register grew by the rod family over the old count.
        check("total register grew beyond the old 7,013 by the new rod signals",
            Instrumentation.size() > 7013,
            "size=" + Instrumentation.size() + " (was 7013)");

        // 7. Rod-type abbreviations are the Soviet ones, verbatim.
        check("rod abbreviations are the regulation's Soviet ones",
            RodType.AZ.abbr.equals("АЗ") && RodType.PK_AZ.abbr.equals("ПК-АЗ")
                && RodType.USP.abbr.equals("УСП") && RodType.LAR.abbr.equals("ЛАР"),
            "АЗ / ПК-АЗ / УСП / ЛАР");

        System.out.printf("%nRodSignalsSelfTest: %d checks, %d failures%n", checks, failures);
        if (failures > 0) {
            throw new AssertionError(failures + " check(s) failed");
        }
    }

    private static long count(java.util.List<Instrumentation.Signal> sigs,
                              Instrumentation.Kind k) {
        return sigs.stream().filter(s -> s.kind() == k).count();
    }

    private static void check(String name, boolean ok, String detail) {
        checks++;
        if (!ok) failures++;
        System.out.printf("  [%s] %s  (%s)%n", ok ? "PASS" : "FAIL", name, detail);
    }
}
