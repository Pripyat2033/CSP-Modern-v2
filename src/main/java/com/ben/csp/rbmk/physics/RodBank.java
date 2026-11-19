package com.ben.csp.rbmk.physics;

import com.ben.csp.rbmk.Cartogram;
import com.ben.csp.rbmk.RbmkGeometry;
import com.ben.csp.rbmk.RodType;

import java.util.List;

/**
 * The 211 control rods as individual objects with individual positions.
 *
 * <p>The core used to carry a single {@code insertion} scalar applied to every rod
 * at once. That is not the RBMK: rods are driven in banks, the operator withdraws
 * some and leaves others, and the whole notion of an operating reactivity margin
 * depends on <em>which</em> rods are where. This class is the per-rod state the
 * margin needs.
 *
 * <p><b>ORM</b> (оперативный запас реактивности) is defined the way SKALA defines
 * it: the combined worth of every rod currently in the core, expressed as an
 * equivalent number of fully inserted manual rods. In symbols,
 * <pre>
 *   ORM = sum_over_rods  W_rod(insertion_rod)  /  W_manual(1.0)
 * </pre>
 * where {@code W_manual(1.0)} is one fully inserted manual rod. The plant's
 * reference values ({@code orm.reference}): nominal 45-48 rods, warning 43, and 30
 * below which operation is not permitted.
 */
public final class RodBank {

    private final RodType[] type;
    private final double[] insertion;
    private final double[] target;

    /** Per-rod worth curve, shared because it is a function of insertion only. */
    private final RodWorth worth;

    /** Worth of one fully inserted manual rod, dk/k — the ORM unit. */
    private final double unitWorth;

    /**
     * A bank of the sourced cartogram's rods, all withdrawn.
     */
    public RodBank() {
        this(Cartogram.types(), new RodWorth());
    }

    public RodBank(List<RodType> types, RodWorth worth) {
        if (types.isEmpty()) {
            throw new IllegalStateException(
                "no control rods: the cartogram resource failed to load or validate");
        }
        this.type = types.toArray(new RodType[0]);
        this.insertion = new double[this.type.length];
        this.target = new double[this.type.length];
        this.worth = worth;
        this.unitWorth = worth.worth(RodType.RR, 1.0);
        if (!(unitWorth < 0)) {
            throw new IllegalStateException("manual rod worth must be negative, got " + unitWorth);
        }
    }

    public int size() {
        return type.length;
    }

    public RodType type(int i) {
        return type[i];
    }

    public double insertion(int i) {
        return insertion[i];
    }

    public double target(int i) {
        return target[i];
    }

    /** Set a rod's target position, 0 = fully withdrawn, 1 = fully inserted. */
    public void setTarget(int i, double t) {
        target[i] = clamp01(t);
    }

    /** Drive every rod of a type toward a target. */
    public void setTarget(RodType t, double v) {
        for (int i = 0; i < type.length; i++) {
            if (type[i] == t) {
                target[i] = clamp01(v);
            }
        }
    }

    /**
     * Advance every rod toward its target at its own type's speed. АЗ rods fall in
     * 2.5 s; the rest take 12 s, so a scram is <em>staggered</em> — the asymmetry
     * that matters for AZ-5.
     *
     * @param dt seconds
     * @param scram whether this is a scram (fast) or a normal drive (slow)
     */
    public void step(double dt, boolean scram) {
        for (int i = 0; i < type.length; i++) {
            double d = target[i] - insertion[i];
            if (d == 0) {
                continue;
            }
            double speed = 1.0 / travelSeconds(type[i], scram);
            double move = Math.min(Math.abs(d), speed * dt);
            insertion[i] += Math.signum(d) * move;
        }
    }

    /** Time for a full stroke of this type, s. */
    public static double travelSeconds(RodType t, boolean scram) {
        if (scram) {
            return t == RodType.AZ ? 2.5 : 12.0;
        }
        return t == RodType.AZ ? 6.5 : 17.5;
    }

    /** Reactivity of one rod at its current position, dk/k. */
    public double rodWorth(int i) {
        return worth.worth(type[i], insertion[i]);
    }

    /** Total reactivity held by the rods, dk/k (negative = net absorption). */
    public double totalWorth() {
        double sum = 0;
        for (int i = 0; i < type.length; i++) {
            sum += rodWorth(i);
        }
        return sum;
    }

    /**
     * The operating reactivity margin, in equivalent fully inserted manual rods —
     * the number SKALA reports for request code {@code K0S0000}.
     */
    public double orm() {
        return totalWorth() / unitWorth;
    }

    /** The worth of one fully inserted manual rod, dk/k. */
    public double unitWorth() {
        return unitWorth;
    }

    /** Mean insertion over all rods — the old scalar, kept for reporting. */
    public double meanInsertion() {
        double s = 0;
        for (double v : insertion) {
            s += v;
        }
        return s / insertion.length;
    }

    /** Number of rods whose absorber is inside the core. */
    public int insertedCount() {
        int n = 0;
        for (double v : insertion) {
            if (v > 0) {
                n++;
            }
        }
        return n;
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }
}
