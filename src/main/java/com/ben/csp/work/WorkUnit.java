package com.ben.csp.work;

/**
 * The units Soviet construction work was measured and paid in.
 *
 * <p>Soviet norms (ЕНиР / СНиП) priced work per unit of physical volume, so a
 * наряд always carried a quantity in one of these units; man-hours are then
 * derived from a norm (see {@link WorkType}).
 */
public enum WorkUnit {
    CUBIC_METRE("m³"),
    SQUARE_METRE("m²"),
    LINEAR_METRE("m"),
    TONNE("t"),
    PIECE("pc"),
    POINT("pt"),      // geodetic survey point
    MAN_HOUR("mh"),
    LUMP_SUM("lump"); // без нормы: agreed as a whole

    private final String symbol;

    WorkUnit(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() {
        return symbol;
    }
}
