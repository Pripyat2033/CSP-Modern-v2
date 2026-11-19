package com.ben.csp.rbmk;

import java.util.ArrayList;
import java.util.List;

/**
 * The seven control-rod types of the second-generation RBMK-1000 core, with the
 * counts taken verbatim from the units 3/4 operating regulation, §2.11.6
 * (<em>Наличие штатных и дополнительных поглотителей в активной зоне</em>):
 *
 * <pre>
 *   12 стержней ЛАР      local automatic regulators
 *   12 стержней АР       automatic regulators
 *   24 стержня  АЗ       emergency protection rods
 *   24 стержня  ПК-АЗ    overcompensation rods for АЗ
 *   12 стержней ПК-РР    overcompensation rods for РР
 *  103 стержня  РР       manual regulators
 *   24 стержня  УСП      shortened absorber rods (inserted from below)
 * </pre>
 *
 * <p>The seven counts sum to exactly <b>211</b> — the control-channel total of the
 * second-generation core — so the inventory is confirmed by the plant's own
 * regulation, not assumed. The earlier model treated all 211 as one undifferentiated
 * rod; this type restores the distinction, which matters because an АЗ rod is not
 * an РР rod and the УСП rods are physically different (shortened, driven from
 * below — see {@link RbmkGeometry#RODS_FROM_BELOW}).
 *
 * <p><b>ПК = перекомпенсация</b> (overcompensation). The regulation's abbreviation
 * list defines «ПК-АЗ, РР — перекомпенсация — АЗ, РР», i.e. the ПК function exists
 * for the АЗ and РР rods; the two dedicated overcompensation rod types carry it.
 */
public enum RodType {

    /** Shortened absorber rod, inserted from below; sits in the most effective zone. */
    USP("УСП", "Shortened absorber rod (from below)", 24,
        4.088, 6.700, true, Double.NaN, Double.NaN),
    /** Emergency protection rod (АЗ). */
    AZ("АЗ", "Emergency protection rod", 24,
        7.247, 0.0, false, -2.0, Double.NaN),
    /** Overcompensation rod for the АЗ rods. */
    PK_AZ("ПК-АЗ", "Overcompensation rod (АЗ)", 24,
        6.772, 4.560, false, Double.NaN, 0.5),
    /** Automatic regulator rod (АР). */
    AR("АР", "Automatic regulator rod", 12,
        7.035, 4.510, false, Double.NaN, 0.5),
    /** Local automatic regulator rod (ЛАР). */
    LAR("ЛАР", "Local automatic regulator rod", 12,
        6.772, 4.560, false, Double.NaN, 0.5),
    /** Overcompensation rod for the РР rods. */
    PK_RR("ПК-РР", "Overcompensation rod (РР)", 12,
        6.772, 4.560, false, Double.NaN, 0.5),
    /** Manual regulator rod (РР). */
    RR("РР", "Manual regulator rod", 103,
        6.772, 4.560, false, Double.NaN, 0.5);

    /** The Soviet abbreviation, verbatim from the regulation. */
    public final String abbr;
    /** An English gloss for the abbreviation. */
    public final String nameEn;
    /** How many rods of this type the regulation specifies. */
    public final int count;

    /**
     * B4C absorber length, m. Per type, from the unit 4 SUZ panel table
     * ({@code rod.lengths}). AZ is a full-core rod; УСП is the shortened one.
     */
    public final double absorberM;
    /** Graphite displacer length, m; 0 for AZ, which has none. */
    public final double displacerM;
    /** Whether the drive enters from below (the УСП rods do). */
    public final boolean fromBelow;
    /** Absorber worth of the whole group, in units of beta_eff; NaN where unsourced. */
    public final double groupWorthBeta;
    /** Displacer worth of the whole group, in units of beta_eff; NaN where unsourced. */
    public final double displacerWorthBeta;

    RodType(String abbr, String nameEn, int count,
            double absorberM, double displacerM, boolean fromBelow,
            double groupWorthBeta, double displacerWorthBeta) {
        this.abbr = abbr;
        this.nameEn = nameEn;
        this.count = count;
        this.absorberM = absorberM;
        this.displacerM = displacerM;
        this.fromBelow = fromBelow;
        this.groupWorthBeta = groupWorthBeta;
        this.displacerWorthBeta = displacerWorthBeta;
    }

    /**
     * The panel's blue "RR" category is 127 rods, which the regulation splits
     * into 103 РР + 12 ПК-РР + 12 ЛАР. All three are manually driven and
     * physically identical (6.772 m absorber, 4.560 m displacer), so they share
     * the manual-rod worth.
     */
    public static final double MANUAL_ROD_WORTH_BETA = 0.5;

    /** Whether this rod type carries an overcompensation (ПК) drive signal. */
    public boolean hasOvercompensation() {
        return this == PK_AZ || this == PK_RR;
    }

    /** The number of control rods the regulation specifies — 211 for the second generation. */
    public static int total() {
        int t = 0;
        for (RodType r : values()) {
            t += r.count;
        }
        return t;
    }

    /**
     * The rod-type cartogram, read from the unit 4 SUZ panel drawing
     * ({@code /rbmk_cartogram_ch4.json}, provenance key {@code rod.cartogram}).
     *
     * <p>The source is the SVG of the unit's control-panel mimic, which carries one
     * <em>rect</em> per channel with an explicit fill per rod type. Transcribing its
     * geometry gives the physical position of all 1,872 channels, so the placement
     * of each rod type is now sourced rather than assumed. Counts reproduce exactly:
     * АЗ 24, ПК-АЗ 24, АР 12, УСП 24, РР 127, ДКЭ 12, ТК 1,661.
     *
     * <p>The panel's single blue "РР" category of 127 rods is the regulation's
     * 103 РР + 12 ПК-РР + 12 ЛАР; all three are manually driven and physically
     * identical, so they map to {@link #RR} here.
     *
     * @return one entry per control channel, in panel row-major order
     */
    public static List<RodType> cartogram() {
        return Cartogram.types();
    }

    /**
     * @deprecated the cartogram no longer depends on a supplied count; use
     *     {@link #cartogram()}. Kept so existing callers compile.
     */
    @Deprecated
    public static List<RodType> cartogram(int controlCount) {
        if (controlCount != total()) {
            throw new IllegalArgumentException(
                "cartogram expects " + total() + " control channels, got " + controlCount);
        }
        return Cartogram.types();
    }
}
