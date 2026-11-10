package com.ben.csp.zone.genplan;

/**
 * The figures a Soviet general plan is worked out to, taken from СНиП II-89-80*
 * "Генеральные планы промышленных предприятий". Every number here is quoted from
 * the norm rather than invented, so the site the mod lays out is a site a Soviet
 * проектировщик would have recognised. The clause is named on each field.
 */
public final class SiteNorms {

    private SiteNorms() {}

    // --- Противопожарные разрывы (fire breaks), табл. 1* --------------------------
    // The clear distance between buildings, by the degree of fire resistance of the
    // two buildings. A construction base is mostly category Д (non-combustible work,
    // degrees I-IIIa): between such buildings the break is not standardised, but the
    // norm gives 9 m as the working figure; stores of timber and the like are lower
    // degrees, so they stand 12-15 m off. We hold every building at the safe figure
    // for its neighbour rather than the 0 the clause allows.
    /** Buildings of degrees I, II, IIIa with productions of categories А, Б, В, Е. */
    public static final double FIRE_BREAK_TIGHT = 9.0;
    /** A building of degree III against degree III. */
    public static final double FIRE_BREAK_STANDARD = 12.0;
    /** A combustible building (degrees IIIб, IV, IVа, V) against degree III. */
    public static final double FIRE_BREAK_WIDE = 15.0;

    // --- Дороги (roads) ----------------------------------------------------------
    // §3.47-3.48: one road per проезд, its width set by the проезд between buildings.
    /** One lane of a two-lane road (табл. 5*, прим. 3: 3.75 m a lane). */
    public static final double ROAD_LANE = 3.5;
    /** A two-lane site road. */
    public static final double ROAD_TWO_LANE = 7.0;
    /** Distance from the road edge to a building wall (табл. 5*, поз. 1б). */
    public static final double ROAD_TO_BUILDING = 3.0;

    // --- Железная дорога (rail) --------------------------------------------------
    // §3.52 and табл. 5*/6: a normal-gauge industrial spur, 1520 mm.
    /** Between the axes of parallel tracks of 1520 mm gauge (табл. 5*, поз. 2). */
    public static final double RAIL_TRACK_SPACING = 3.75;
    /** The rail corridor: a running spur and a siding beside it. */
    public static final double RAIL_CORRIDOR = 2 * RAIL_TRACK_SPACING + 1.0;
    /** From the axis of an internal rail spur to a building with exits (табл. 6, поз. 1б). */
    public static final double RAIL_TO_BUILDING = 6.0;

    // --- Плотность застройки (building density) ----------------------------------
    // Приложение, "Показатели минимальной плотности застройки": for the enterprises of
    // Минстройматериалов / Минтяжстрой (concrete and reinforced-concrete works, the
    // trade of this base) the minimum is 50 %. The plan must not spread the site out
    // below this: a Soviet genplan is compact by requirement, not by taste.
    /** The minimum building density of the site, as a fraction. */
    public static final double DENSITY_MIN = 0.50;

    // --- Зоны (the layout of the site) -------------------------------------------
    /** The lane left between plots (a пешеходная дорожка / служебный проезд). */
    public static final double LANE = 4.0;
    /** A sanitary break between the working zones and the предзаводская zone. */
    public static final double SANITARY_BREAK = 12.0;

    /**
     * The clear gap between rows of buildings in a zone. Buildings of degrees I-IIIa
     * with non-combustible work (categories Г, Д — most of a construction base) have no
     * standardised break (табл. 1*), so a service lane of 6 m is used; the storage zone
     * holds combustible materials (timber, cable), so it is held at the 12 m break of a
     * degree III building (табл. 1*).
     */
    public static double rowGap(Zone zone) {
        return zone == Zone.STORAGE ? FIRE_BREAK_STANDARD : 6.0;
    }
}
