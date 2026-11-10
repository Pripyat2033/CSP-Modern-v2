package com.ben.csp.zone.genplan;

import com.ben.csp.enterprise.FacilityType;

/**
 * The functional zones of an industrial site.
 *
 * <p>SNiP II-89-80 §3.8 (Планировка территории): "По функциональному
 * использованию площадку предприятия следует разделять на зоны: а)
 * предзаводскую; б) производственную; в) подсобную; г) складскую". A Soviet
 * general plan groups the site by what each part does, not by who owns it, so that
 * like work sits together and the transport, sanitary and fire requirements of
 * §3.3 can be met. A facility is therefore placed in the zone its work belongs to.
 */
public enum Zone {

    PREDZAVODSKAYA("Предзаводская", "Administration and domestic"),
    PRODUCTION("Производственная", "Production"),
    AUXILIARY("Подсобная", "Auxiliary and transport"),
    STORAGE("Складская", "Storage");

    private final String russian;
    private final String label;

    Zone(String russian, String label) {
        this.russian = russian;
        this.label = label;
    }

    public String russian() { return russian; }
    public String label() { return label; }

    /** The zone a facility's work belongs to. */
    public static Zone of(FacilityType type) {
        return switch (type) {
            case HEADQUARTERS, CONSTRUCTION_OFFICE -> PREDZAVODSKAYA;
            case CONCRETE_PLANT, REBAR_PLANT, WORKSHOP -> PRODUCTION;
            case TRANSPORT_DEPOT, EXCAVATION_DEPOT -> AUXILIARY;
            case STORAGE_YARD, STEEL_YARD, CABLE_STORE, PILE_DEPOT -> STORAGE;
        };
    }
}
