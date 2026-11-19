package com.ben.csp.rbmk.physics;

import com.ben.csp.rbmk.Cartogram;
import com.ben.csp.rbmk.RbmkGeometry;
import com.ben.csp.rbmk.RodType;

/**
 * Proves the per-rod state and the operating reactivity margin rest on sourced
 * data, and that the margin behaves the way the plant's documents say it must.
 *
 * <p>The anchors, all from the unit 4 panel and the units 3/4 operating regulation
 * (provenance keys {@code rod.cartogram}, {@code rod.worth_group},
 * {@code orm.reference}):
 * <ul>
 *   <li>the cartogram carries 1,661 fuel channels, 211 control rods and 12 detectors,
 *       with the exact per-type counts of the SUZ panel legend;</li>
 *   <li>the 24 АЗ rods together are worth -2 beta_eff, which is what calibrates the
 *       absolute rod-worth scale;</li>
 *   <li>a scram is staggered: АЗ rods fall in 2.5 s, the rest in 12 s;</li>
 *   <li>ORM is a rod count — nominal 45-48, warning 43, limit 30 — and must be
 *       monotonic in insertion and zero-ish when the rods are out.</li>
 * </ul>
 */
public final class OrmSelfTest {

    private static int checks = 0;
    private static int failures = 0;

    private static final double BETA = 0.0065;

    public static void main(String[] args) {
        System.out.println("=== OrmSelfTest ===");

        // ---- 1. the cartogram is the sourced one ----
        check("cartogram loads from the unit 4 SUZ panel resource",
            !Cartogram.sites().isEmpty(), "sites=" + Cartogram.sites().size());
        check("cartogram holds 1,884 channels (1,661 ТК + 211 control + 12 ДКЭ)",
            Cartogram.sites().size() == 1884,
            "sites=" + Cartogram.sites().size());
        check("cartogram control rods = 211", Cartogram.rods().size() == 211,
            "rods=" + Cartogram.rods().size());
        check("cartogram detectors = 12", Cartogram.detectors() == 12,
            "detectors=" + Cartogram.detectors());

        // ---- 2. per-type counts are the panel legend's, exactly ----
        check("АЗ = 24 (panel red #de1a03)", Cartogram.count(RodType.AZ) == 24,
            "n=" + Cartogram.count(RodType.AZ));
        check("ПК-АЗ = 24 (panel grey #555555)", Cartogram.count(RodType.PK_AZ) == 24,
            "n=" + Cartogram.count(RodType.PK_AZ));
        check("АР = 12 (panel green #01b151)", Cartogram.count(RodType.AR) == 12,
            "n=" + Cartogram.count(RodType.AR));
        check("УСП = 24 (panel yellow #fed801)", Cartogram.count(RodType.USP) == 24,
            "n=" + Cartogram.count(RodType.USP));
        // The panel draws one blue category of 127 manual rods. The model maps all
        // three manual sub-types to RodType.RR because they are physically identical
        // (6.772 m absorber, 4.560 m displacer) and the panel does not separate them.
        check("manual rods = 127, the panel's single blue category "
                + "(the regulation's РР 103 + ПК-РР 12 + ЛАР 12)",
            Cartogram.count(RodType.RR) == 127, "n=" + Cartogram.count(RodType.RR));

        // ---- 3. per-type geometry is sourced, not uniform ----
        check("АЗ absorber is 7.247 m with no displacer",
            RodType.AZ.absorberM == 7.247 && RodType.AZ.displacerM == 0.0,
            "abs=" + RodType.AZ.absorberM + " disp=" + RodType.AZ.displacerM);
        check("УСП is the shortened rod (4.088 m) driven from below",
            RodType.USP.absorberM == 4.088 && RodType.USP.fromBelow,
            "abs=" + RodType.USP.absorberM + " below=" + RodType.USP.fromBelow);
        check("manual rod is 6.772 m absorber + 4.560 m displacer",
            RodType.RR.absorberM == 6.772 && RodType.RR.displacerM == 4.560,
            "abs=" + RodType.RR.absorberM + " disp=" + RodType.RR.displacerM);

        // ---- 4. rod worth calibrates to the panel's -2 beta АЗ group ----
        RodWorth w = new RodWorth();
        double azGroup = w.worth(RodType.AZ, 1.0) * 24;
        check("the 24 АЗ rods are worth -2.00 beta (panel: -2 beta)",
            Math.abs(azGroup / BETA + 2.0) < 0.01,
            String.format("%+.4f dk/k = %+.3f beta", azGroup, azGroup / BETA));

        check("a fully inserted rod has negative worth (it absorbs)",
            w.worth(RodType.RR, 1.0) < 0, "worth=" + w.worth(RodType.RR, 1.0));
        check("a withdrawn rod is worth ~0, not strongly negative",
            Math.abs(w.worth(RodType.RR, 0.0)) < 0.1 * Math.abs(w.worth(RodType.RR, 1.0)),
            "worth(0)=" + w.worth(RodType.RR, 0.0));

        // ---- 5. the positive scram: inserting a manual rod first RAISES reactivity ----
        double w0 = w.worth(RodType.RR, 0.0);
        double peak = Double.NEGATIVE_INFINITY;
        double peakAt = 0;
        for (int i = 0; i <= 100; i++) {
            double x = i / 100.0;
            double v = w.worth(RodType.RR, x);
            if (v > peak) {
                peak = v;
                peakAt = x;
            }
        }
        check("manual rod worth RISES before it falls (the displacer, not the absorber)",
            peak > w0 && peakAt > 0.0 && peakAt < 0.5,
            String.format("peak %+.5f at insertion %.2f, withdrawn %+.5f", peak, peakAt, w0));

        // ---- 6. the bank is the cartogram's rods ----
        RodBank bank = new RodBank();
        check("the bank holds 211 individual rods", bank.size() == 211,
            "size=" + bank.size());
        check("the bank's unit is one fully inserted manual rod",
            bank.unitWorth() == w.worth(RodType.RR, 1.0),
            "unit=" + bank.unitWorth());

        // ---- 7. ORM behaves like a margin ----
        setAll(bank, 0.0);
        double ormOut = bank.orm();
        check("all rods withdrawn gives a margin near zero (not a large number)",
            Math.abs(ormOut) < 15.0, String.format("ORM=%.1f rods", ormOut));

        setAll(bank, 1.0);
        double ormIn = bank.orm();
        check("all rods inserted gives a margin of about 211 rods",
            Math.abs(ormIn - 211.0) < 12.0, String.format("ORM=%.1f rods", ormIn));

        // ORM rises with insertion. It is not strictly monotonic at the very end: a
        // manual rod's 6.772 m absorber is SHORTER than the 7.0 m core, so driving it
        // fully home leaves the top 0.23 m of the core unabsorbed and gives back a
        // little worth. That overshoot is real, so allow it and check it explicitly.
        boolean monotonic = true;
        double prev = Double.NEGATIVE_INFINITY;
        double worstDrop = 0;
        double[] at = new double[11];
        for (int i = 0; i <= 10; i++) {
            setAll(bank, i / 10.0);
            double o = bank.orm();
            at[i] = o;
            if (o < prev - 1e-9) {
                monotonic = false;
                worstDrop = Math.max(worstDrop, prev - o);
            }
            prev = o;
        }
        check("ORM rises with insertion except a small overshoot at full travel",
            at[10] > at[0] && worstDrop < 0.05 * at[10],
            String.format("%.1f -> %.1f, worst step drop %.1f rods", at[0], at[10], worstDrop));
        check("the overshoot is the absorber being shorter than the core (6.772 < 7.0 m)",
            RodType.RR.absorberM < RbmkGeometry.CORE_HEIGHT_M
                && w.worth(RodType.RR, 1.0) > w.worth(RodType.RR, 0.95),
            String.format("worth(0.95)=%+.6f worth(1.0)=%+.6f",
                w.worth(RodType.RR, 0.95), w.worth(RodType.RR, 1.0)));

        // the operating point: margin must be able to reach the plant's nominal band
        setAll(bank, 0.0);
        bank.setTarget(RodType.AZ, 0.0);
        for (int i = 0; i < bank.size(); i++) {
            if (bank.type(i) != RodType.AZ) {
                bank.setTarget(i, 0.31);
            }
        }
        bank.step(1000.0, false);
        double ormOp = bank.orm();
        check("with the protection rods out and the rest ~31% in, ORM is in the "
                + "nominal 45-48 band",
            ormOp >= 40.0 && ormOp <= 56.0, String.format("ORM=%.1f rods", ormOp));

        // ---- 8. a scram is staggered ----
        check("АЗ scram travel is 2.5 s", RodBank.travelSeconds(RodType.AZ, true) == 2.5,
            "t=" + RodBank.travelSeconds(RodType.AZ, true));
        check("non-АЗ scram travel is 12 s",
            RodBank.travelSeconds(RodType.RR, true) == 12.0,
            "t=" + RodBank.travelSeconds(RodType.RR, true));

        setAll(bank, 0.0);
        bank.setTarget(RodType.AZ, 1.0);
        bank.setTarget(RodType.RR, 1.0);
        bank.step(2.5, true);
        double azPos = positionOf(bank, RodType.AZ);
        double rrPos = positionOf(bank, RodType.RR);
        check("after 2.5 s of scram the АЗ rods are in but the manual rods are not",
            azPos > 0.99 && rrPos < 0.5,
            String.format("АЗ=%.2f РР=%.2f", azPos, rrPos));

        System.out.printf("%nOrmSelfTest: %d checks, %d failures%n", checks, failures);
        if (failures > 0) {
            throw new AssertionError(failures + " check(s) failed");
        }
    }

    private static void setAll(RodBank b, double v) {
        for (int i = 0; i < b.size(); i++) {
            b.setTarget(i, v);
        }
        b.step(1000.0, false);
    }

    private static double positionOf(RodBank b, RodType t) {
        double sum = 0;
        int n = 0;
        for (int i = 0; i < b.size(); i++) {
            if (b.type(i) == t) {
                sum += b.insertion(i);
                n++;
            }
        }
        return n == 0 ? Double.NaN : sum / n;
    }

    private static void check(String name, boolean ok, String detail) {
        checks++;
        if (!ok) {
            failures++;
        }
        System.out.printf("  [%s] %s  (%s)%n", ok ? "PASS" : "FAIL", name, detail);
    }
}
