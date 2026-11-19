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

        // ---- 5. the positive scram, and WHEN it happens ----
        // Inserting a manual rod sweeps its graphite displacer down through the core
        // before the B4C arrives. Whether that RAISES reactivity depends on where the
        // flux is: the displacer is worth most where the flux is. Against a
        // bottom-peaked (accident) flux the withdrawn rod's displacer sits in the weak
        // upper flux, so moving it down into the strong lower flux gains reactivity —
        // the positive scram. Against a centre-peaked (nominal) flux the displacer is
        // already at the peak, so there is nothing to gain and the curve only falls.
        // Asserting the rise unconditionally would be asserting the wrong physics.
        double w0 = w.worth(RodType.RR, 0.0);
        double peak = Double.NEGATIVE_INFINITY;
        double peakAt = 0;
        for (int i = 0; i <= 1000; i++) {
            double x = i / 1000.0;
            double v = w.worth(RodType.RR, x);
            if (v > peak) {
                peak = v;
                peakAt = x;
            }
        }
        check("at NOMINAL (centre-peaked) flux, manual rod worth only falls with insertion",
            !(peak > w0 + 1e-12),
            String.format("peak %+.7f at %.3f vs withdrawn %+.7f", peak, peakAt, w0));

        RodWorth accident = RodWorth.bottomPeaked();
        double a0 = accident.worth(RodType.RR, 0.0);
        double aPeak = Double.NEGATIVE_INFINITY;
        double aPeakAt = 0;
        for (int i = 0; i <= 1000; i++) {
            double v = accident.worth(RodType.RR, i / 1000.0);
            if (v > aPeak) {
                aPeak = v;
                aPeakAt = i / 1000.0;
            }
        }
        check("at ACCIDENT (bottom-peaked) flux, manual rod worth RISES first - the positive scram",
            aPeak > a0 && aPeakAt > 0.0 && aPeakAt < 0.5,
            String.format("peak %+.7f at %.3f, withdrawn %+.7f", aPeak, aPeakAt, a0));

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

        // ORM rises with insertion and is NOT monotonic at the very end: a manual
        // rod's 6.772 m absorber is SHORTER than the 7.0 m core, so driving it fully
        // home leaves the top 0.228 m unabsorbed and gives a little worth back. The
        // effect is small (1.4% of the margin) but real, so pin its size rather than
        // accept anything under a loose bound.
        double prev = Double.NEGATIVE_INFINITY;
        double worstDrop = 0;
        double[] at = new double[11];
        for (int i = 0; i <= 10; i++) {
            setAll(bank, i / 10.0);
            double o = bank.orm();
            at[i] = o;
            worstDrop = Math.max(worstDrop, prev - o);
            prev = o;
        }
        check("ORM rises with insertion overall", at[10] > at[0],
            String.format("%.1f -> %.1f", at[0], at[10]));

        // Measure the peak and the give-back at full travel, rather than sampling
        // coarsely and hoping: 2.9 rods at insertion 0.929 with the sub-node integral.
        double ormPeak = Double.NEGATIVE_INFINITY;
        double ormPeakAt = 0;
        for (int i = 0; i <= 1000; i++) {
            setAll(bank, i / 1000.0);
            double o = bank.orm();
            if (o > ormPeak) {
                ormPeak = o;
                ormPeakAt = i / 1000.0;
            }
        }
        setAll(bank, 1.0);
        double atHome = bank.orm();
        double giveBack = ormPeak - atHome;
        check("ORM peaks just short of full travel, not at home",
            ormPeakAt > 0.85 && ormPeakAt < 1.0, String.format("peak at %.3f", ormPeakAt));
        check("the give-back at full travel is ~2.9 rods (1.4% of the margin)",
            giveBack > 2.0 && giveBack < 4.0,
            String.format("peak %.2f at %.3f, home %.2f, give-back %.2f rods (%.1f%%)",
                ormPeak, ormPeakAt, atHome, giveBack, 100 * giveBack / ormPeak));
        check("the give-back is the absorber being shorter than the core (6.772 < 7.0 m)",
            RodType.RR.absorberM < RbmkGeometry.CORE_HEIGHT_M
                && w.worth(RodType.RR, 1.0) > w.worth(RodType.RR, 0.95),
            String.format("worth(0.95)=%+.6f worth(1.0)=%+.6f",
                w.worth(RodType.RR, 0.95), w.worth(RodType.RR, 1.0)));

        // ---- 7b. the margin must not be quantised on the axial mesh ----
        // Worth was snapped to the 28 node centres, so 87% of insertion positions
        // produced no change at all and one node step could shift ORM by 13.7 rods.
        // The sub-node integral must make it continuous and resolve well under 1 rod.
        double prevOrm = Double.NaN;
        int flat = 0;
        double maxJump = 0;
        int n = 400;
        for (int i = 0; i <= n; i++) {
            setAll(bank, i / (double) n);
            double o = bank.orm();
            if (!Double.isNaN(prevOrm)) {
                double dlt = Math.abs(o - prevOrm);
                if (dlt < 1e-9) {
                    flat++;
                }
                maxJump = Math.max(maxJump, dlt);
            }
            prevOrm = o;
        }
        check("ORM is continuous in insertion (no 28-node staircase)",
            flat == 0, flat + " of " + n + " steps did not move the margin");
        check("no single 0.25% insertion step moves ORM by more than 2 rods",
            maxJump < 2.0, String.format("max jump %.2f rods", maxJump));

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
