package com.ben.csp.work;

/**
 * A trade operation with its Soviet time norm.
 *
 * <p>{@link #manHoursPerUnit} is a {@code ЧАС.ЧЕЛ} figure (man-hours of labour
 * needed per one unit of {@link #unit}). A figure read out of an ЕНиР collection
 * is {@link NormSource#ENIR_SOURCED} and is named in {@link #sourceNote}. A
 * figure that has only the right shape is {@link NormSource#PROVISIONAL}: it
 * carries a {@link #pendingCollection} naming the collection it must be replaced
 * from, and {@link #isProvisional()} says so, so it cannot be mistaken for a
 * sourced number. Nothing downstream may present a provisional norm as final.
 */
public enum WorkType {
    EXCAVATION("Excavation", WorkUnit.CUBIC_METRE, 0.60,
        NormSource.PROVISIONAL, "ЕНиР сб. 2 «Земляные работы»",
        "mechanical excavation of soil, per 100 m3"),
    CONCRETE_PLACING("Concrete placing", WorkUnit.CUBIC_METRE, 2.40,
        NormSource.PROVISIONAL, "ЕНиР сб. 4 «Бетонные работы»",
        "placing structural concrete, per m3"),
    REBAR_INSTALLATION("Rebar installation", WorkUnit.TONNE, 18.0,
        NormSource.PROVISIONAL, "ЕНиР сб. 4 «Бетонные работы»",
        "fixing reinforcement, per tonne"),
    BRICKLAYING("Bricklaying", WorkUnit.CUBIC_METRE, 6.00,
        NormSource.PROVISIONAL, "ЕНиР сб. 3 «Каменные работы»",
        "brick masonry, per m3"),
    FORMWORK("Formwork", WorkUnit.SQUARE_METRE, 0.80,
        NormSource.PROVISIONAL, "ЕНиР сб. 4 «Бетонные работы»",
        "erecting and stripping formwork, per m2"),
    STEEL_ERECTION("Steel erection", WorkUnit.TONNE, 12.0,
        NormSource.PROVISIONAL, "ЕНиР сб. 5 «Монтаж металлоконструкций»",
        "erecting steel members, per tonne"),
    CABLE_PULLING("Cable pulling", WorkUnit.LINEAR_METRE, 0.25,
        NormSource.PROVISIONAL, "ЕНиР сб. 23 «Электромонтажные работы»",
        "pulling cable, per linear metre"),
    GEODETIC_SURVEY("Geodetic survey", WorkUnit.POINT, 1.50,
        NormSource.PROVISIONAL, "ЕНиР сб. 1 «Геодезические работы»",
        "setting out a survey point, per point"),
    LUMP("Lump work", WorkUnit.LUMP_SUM, 0.0,
        NormSource.NOT_APPLICABLE, null,
        "agreed as a whole; no per-unit norm applies");

    private final String label;
    private final WorkUnit unit;
    private final double manHoursPerUnit;
    private final NormSource source;
    private final String sourceNote;
    private final String basis;

    WorkType(String label, WorkUnit unit, double manHoursPerUnit,
             NormSource source, String sourceNote, String basis) {
        this.label = label;
        this.unit = unit;
        this.manHoursPerUnit = manHoursPerUnit;
        this.source = source;
        this.sourceNote = sourceNote;
        this.basis = basis;
    }

    public String label() {
        return label;
    }

    public WorkUnit unit() {
        return unit;
    }

    public double manHoursPerUnit() {
        return manHoursPerUnit;
    }

    /** Where the norm comes from (sourced, provisional, or not applicable). */
    public NormSource source() {
        return source;
    }

    public boolean isProvisional() {
        return source == NormSource.PROVISIONAL;
    }

    public boolean isSourced() {
        return source == NormSource.ENIR_SOURCED;
    }

    /** The collection the figure was read from, or is still waiting on. */
    public String sourceNote() {
        return sourceNote;
    }

    /** For a provisional norm, the ЕНиР collection it must be replaced from. */
    public String pendingCollection() {
        return isProvisional() ? sourceNote : null;
    }

    /** What the norm is priced against, in words. */
    public String basis() {
        return basis;
    }

    /**
     * A figure ready to be trusted in a settlement: sourced, or without a per-unit
     * norm at all. A provisional norm is never settled against.
     */
    public boolean isTrustedForSettlement() {
        return source != NormSource.PROVISIONAL;
    }
}
