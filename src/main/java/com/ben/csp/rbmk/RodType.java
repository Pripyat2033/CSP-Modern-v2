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
    USP("УСП", "Shortened absorber rod (from below)", 24),
    /** Emergency protection rod (АЗ). */
    AZ("АЗ", "Emergency protection rod", 24),
    /** Overcompensation rod for the АЗ rods. */
    PK_AZ("ПК-АЗ", "Overcompensation rod (АЗ)", 24),
    /** Automatic regulator rod (АР). */
    AR("АР", "Automatic regulator rod", 12),
    /** Local automatic regulator rod (ЛАР). */
    LAR("ЛАР", "Local automatic regulator rod", 12),
    /** Overcompensation rod for the РР rods. */
    PK_RR("ПК-РР", "Overcompensation rod (РР)", 12),
    /** Manual regulator rod (РР). */
    RR("РР", "Manual regulator rod", 103);

    /** The Soviet abbreviation, verbatim from the regulation. */
    public final String abbr;
    /** An English gloss for the abbreviation. */
    public final String nameEn;
    /** How many rods of this type the regulation specifies. */
    public final int count;

    RodType(String abbr, String nameEn, int count) {
        this.abbr = abbr;
        this.nameEn = nameEn;
        this.count = count;
    }

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
     * The rod-type cartogram: one entry per control channel, innermost channel
     * first, so the order matches {@link RbmkLattice#channels()} for the control
     * cells.
     *
     * <p><b>Placeholder, TO VERIFY.</b> The exact cartogram — which physical cell
     * holds an АЗ versus an РР — is not yet sourced from a primary drawing, so the
     * types are laid out in fixed radial bands here (УСП and the protection rods
     * innermost, the 103 manual regulators outermost). The <em>counts</em> are
     * exact and sourced; only the <em>placement</em> is provisional. The code says
     * so rather than hiding it.
     */
    public static List<RodType> cartogram(int controlCount) {
        if (controlCount != total()) {
            throw new IllegalArgumentException(
                "cartogram expects " + total() + " control channels, got " + controlCount);
        }
        List<RodType> out = new ArrayList<>(controlCount);
        for (RodType t : values()) {
            for (int i = 0; i < t.count; i++) {
                out.add(t);
            }
        }
        return out;
    }
}
