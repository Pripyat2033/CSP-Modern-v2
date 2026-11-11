package com.ben.csp.zone;

import com.ben.csp.work.NormSource;

/**
 * How much material one man-hour of construction work consumes, and where that
 * figure comes from.
 *
 * <p>A Soviet site planned materials from the сметные нормы (СНиП IV) per unit of
 * work, not per man-hour. The relation used here is the provisional bridge: a
 * figure in {@code units per man-hour}, marked {@link NormSource#PROVISIONAL}
 * until it is replaced by a per-unit consumption read from the сметные нормы. The
 * site reports it as provisional, so a plan built on it is known to be a draft.
 */
public final class MaterialNorm {

    private final double unitsPerManHour;
    private final NormSource source;
    private final String note;

    public MaterialNorm(double unitsPerManHour, NormSource source, String note) {
        if (unitsPerManHour < 0) {
            throw new IllegalArgumentException("material norm must not be negative");
        }
        this.unitsPerManHour = unitsPerManHour;
        this.source = source;
        this.note = note;
    }

    /** The shape-correct default used until the сметные нормы are read in. */
    public static MaterialNorm provisional(double unitsPerManHour, String note) {
        return new MaterialNorm(unitsPerManHour, NormSource.PROVISIONAL, note);
    }

    public double unitsPerManHour() {
        return unitsPerManHour;
    }

    public NormSource source() {
        return source;
    }

    public String note() {
        return note;
    }

    public boolean isProvisional() {
        return source == NormSource.PROVISIONAL;
    }

    /** Material needed for a quantity of labour. */
    public double forManHours(double manHours) {
        if (manHours < 0) {
            throw new IllegalArgumentException("man-hours must not be negative");
        }
        return manHours * unitsPerManHour;
    }

    @Override
    public String toString() {
        return unitsPerManHour + " units/man-hour (" + source + ")";
    }
}
