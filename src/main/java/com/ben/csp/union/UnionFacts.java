package com.ben.csp.union;

import com.ben.csp.enterprise.Provenance;

/**
 * What is verified about how a Soviet enterprise of this branch met the rest of
 * the country. Source: Russian Wikipedia, articles "Госплан СССР",
 * "Госснаб СССР", "Материально-техническое снабжение",
 * "Министерство энергетики и электрификации СССР", "Хозяйственный расчёт".
 */
public final class UnionFacts {

    private UnionFacts() {}

    public static final String SOURCE =
        "ru.wikipedia: Госплан СССР; Госснаб СССР; Материально-техническое снабжение; "
        + "Министерство энергетики и электрификации СССР; Хозяйственный расчёт; "
        + "Янов (станция); Железнодорожная линия Чернигов — Овруч";

    /**
     * A verified fact worth teaching: the existence of the Chernihiv-Ovruch rail
     * line was one of the main reasons the ChNPP site was chosen there at all.
     * Source: ru.wikipedia, Железнодорожная линия Чернигов — Овруч.
     */
    public static final String SITE_CHOSEN_FOR_THE_RAIL =
        "Именно наличие железнодорожной линии Чернигов — Овруч послужило одной из "
        + "главных причин в выборе площадки под строительство ЧАЭС.";

    /** The branch ministry for a power station: Minenergo, in Moscow. */
    public static Authority minenergo() {
        return new Authority("minenergo", AuthorityKind.MINISTRY,
            "Министерство энергетики и электрификации СССР", AdministrativeLevel.USSR,
            Provenance.VERIFIED, SOURCE);
    }

    /** The state planning committee, whose plan is tiered down to the city. */
    public static Authority gosplan(AdministrativeLevel level) {
        return new Authority("gosplan-" + level.name().toLowerCase(), AuthorityKind.GOSPLAN,
            "Госплан (" + level.label() + ")", level, Provenance.VERIFIED, SOURCE);
    }

    /** The state committee for material-technical supply. */
    public static Authority gossnab() {
        return new Authority("gossnab", AuthorityKind.GOSSNB,
            "Госснаб СССР", AdministrativeLevel.USSR, Provenance.VERIFIED, SOURCE);
    }

    /**
     * The rail link that served the plant: Yanov station, on the Chernihiv-Ovruch
     * section of the South-Western Railway. Before the accident it did passenger
     * and freight work and the plant's own access lines joined it there, as did
     * the ORS warehouses and the oil depot. Source: ru.wikipedia, Янов (станция).
     */
    public static TransportNode yanovRail() {
        return new TransportNode("yanov", "Станция Янов (Yanov railway station)",
            TransportKind.RAIL, "Юго-Западная ж.д., участок Чернигов — Овруч",
            Provenance.VERIFIED, SOURCE);
    }

    /**
     * The Pripyat river port, on the Pripyat river. Its existence is known, but
     * the detail (berths, the craft that called) is not yet sourced, so it is
     * marked a reconstruction until it is.
     */
    public static TransportNode pripyatRiverPort() {
        return new TransportNode("pripyat-port", "Речной порт Припять (Pripyat river port)",
            TransportKind.RIVER, "река Припять (Pripyat river)",
            Provenance.RECONSTRUCTION,
            "Ben (Pripyat2033); detail unsourced");
    }

    /**
     * A supply interface for a power-branch enterprise: subordinate to Minenergo,
     * with the plan from GOSPLAN and materials from GOSSNB, connected by rail.
     */
    public static EnterpriseInterface powerBranchInterface(String enterpriseId) {
        return new EnterpriseInterface(enterpriseId, true)
            .to(minenergo())
            .to(gosplan(AdministrativeLevel.USSR))
            .to(gossnab())
            .via(yanovRail());
    }
}
