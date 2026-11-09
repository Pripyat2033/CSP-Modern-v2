package com.ben.csp.work;

/**
 * A trade operation with its Soviet time norm.
 *
 * <p>{@link #manHoursPerUnit} is a {@code ЧАС.ЧЕЛ} figure (man-hours of labour
 * needed per one unit of {@link #unit}). Real values come from the ЕНиР
 * collections; the constants here are placeholders that keep the shape correct
 * and must be replaced with sourced figures before they are trusted.
 */
public enum WorkType {
    EXCAVATION("Excavation",            WorkUnit.CUBIC_METRE, 0.60),
    CONCRETE_PLACING("Concrete placing", WorkUnit.CUBIC_METRE, 2.40),
    REBAR_INSTALLATION("Rebar installation", WorkUnit.TONNE, 18.0),
    BRICKLAYING("Bricklaying",           WorkUnit.CUBIC_METRE, 6.00),
    FORMWORK("Formwork",                 WorkUnit.SQUARE_METRE, 0.80),
    STEEL_ERECTION("Steel erection",     WorkUnit.TONNE, 12.0),
    CABLE_PULLING("Cable pulling",       WorkUnit.LINEAR_METRE, 0.25),
    GEODETIC_SURVEY("Geodetic survey",   WorkUnit.POINT, 1.50),
    LUMP("Lump work",                    WorkUnit.LUMP_SUM, 0.0);

    private final String label;
    private final WorkUnit unit;
    private final double manHoursPerUnit;

    WorkType(String label, WorkUnit unit, double manHoursPerUnit) {
        this.label = label;
        this.unit = unit;
        this.manHoursPerUnit = manHoursPerUnit;
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
}
