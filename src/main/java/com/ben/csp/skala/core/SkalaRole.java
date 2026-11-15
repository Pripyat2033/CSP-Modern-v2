package com.ben.csp.skala.core;

/**
 * The operating personnel of a ChNPP second-stage unit, by their <b>Soviet
 * abbreviations</b> — the "S positions". These are taken verbatim from the
 * plant's own regulation, <i>Технологический регламент эксплуатации АЭС с
 * реакторами РБМК-1000, 1Э-С-II-1983</i> (ЧАЭС), Chapter 1 ("Общие положения")
 * and the abbreviation list on page 8. They are not invented and not translated
 * loosely: the abbreviation is the role.
 *
 * <p>The pattern the request is about: many of these begin with <b>С</b> =
 * <i>старший</i> (senior) — СИУБ, СИУР, СИУТ, СИМ — while the shift-command
 * roles begin with <b>Н</b> = <i>начальник</i> (chief) — НСС, НСЦ, НСБ. So "SDVIT"
 * is read as <i>старший</i> + a role abbreviation, the same way СИУР is
 * <i>старший</i> + ИУР.
 *
 * <p>Each role is tied to its <b>workplace</b> (ЦЩУ, БЩУ-О, or a specific pult).
 * This matters for the reconstruction because in the control room it is the
 * <i>pult</i> a person stands at, and the request device beside it, that define
 * the role — not a job title on a wall.
 */
public enum SkalaRole {

    /** ГИС — главный инженер станции. */
    GIS("ГИС", "главный инженер станции", "Chief engineer of the station",
        "—", "Ultimate technical authority on site"),

    /** ЗГИС — заместитель главного инженера. */
    ZGIS("ЗГИС", "заместитель главного инженера", "Deputy chief engineer",
        "—", "Deputy for operations / for science"),

    /** НСС — начальник смены станции. General operational command of the whole
     *  second stage, in all modes. */
    NSS("НСС", "начальник смены станции", "Station shift chief",
        "ЦЩУ", "General operational command of the plant, all modes"),

    /** ЗНСС — заместитель начальника смены станции. Direct operational command
     *  of a unit's equipment; sits at БЩУ-О. */
    ZNSS("ЗНСС", "заместитель начальника смены станции", "Deputy station shift chief",
        "БЩУ-О", "Direct operational command of the unit's equipment; orders bind all shift staff"),

    /** НСБ — начальник смены блока. */
    NSB("НСБ", "начальник смены блока", "Unit shift chief",
        "БЩУ-О", "Shift chief of one unit"),

    /** НСЦ — начальник смены цеха. */
    NSC("НСЦ", "начальник смены цеха", "Shop/workshop shift chief",
        "shop", "Shift chief of a shop (РЦ, ТЦ, ХЦ, ЭЦ, ЦТАИ, ЦНиИ)"),

    /** НСЭЦ — начальник смены электроцеха. Controls planned electrical switching. */
    NSEZ("НСЭЦ", "начальник смены электроцеха", "Electrical shop shift chief",
        "ЭЦ", "Controls planned switching on the electrical side"),

    /** СИУБ — старший инженер управления блоком. Sits at pults "П" and "Д". */
    SIUB("СИУБ", "старший инженер управления блоком", "Senior unit-control engineer",
        "pult П, Д (БЩУ-О)", "Unit equipment modes (КМПЦ, deaerator-feed and steam-dump); coordinates СИУР and СИУТ"),

    /** СИУР — старший инженер управления реактором. Sits at pult "А". */
    SIUR("СИУР", "старший инженер управления реактором", "Senior reactor-control engineer",
        "pult А (БЩУ-О)", "Direct control of the reactor: graphite temperature, moisture, КЦТК, regulation/control/protection, per-channel flow"),

    /** СИУТ — старший инженер управления турбинами. Sits at pult "Т". */
    SIUT("СИУТ", "старший инженер управления турбинами", "Senior turbine-control engineer",
        "pult Т (БЩУ-О)", "Turbines, auxiliaries, generators and unit house loads"),

    /** СИМ — старший инженер-механик. */
    SIM("СИМ", "старший инженер-механик", "Senior mechanical engineer",
        "—", "Mechanical engineering of the shift"),

    /** The senior duty electrician on the main scheme (regulation 1.6). Not given
     *  an abbreviation in the personnel list, but named as a duty role. */
    DUTY_ELECTRICIAN("—", "старший дежурный электромонтер по главной схеме",
        "Senior duty electrician, main scheme",
        "ЦЩУ", "Direct control of the main electrical scheme, under the НСЭЦ");

    public final String abbr;
    public final String ru;
    public final String en;
    public final String workplace;
    public final String authority;

    SkalaRole(String abbr, String ru, String en, String workplace, String authority) {
        this.abbr = abbr; this.ru = ru; this.en = en;
        this.workplace = workplace; this.authority = authority;
    }

    /** Look a role up by its abbreviation (case-insensitive). */
    public static SkalaRole byAbbr(String a) {
        for (SkalaRole r : values()) {
            if (r.abbr.equalsIgnoreCase(a)) return r;
        }
        return null;
    }

    /**
     * The three control-room operating posts, each with its own request device,
     * as the source describes them: VIUR / VIUB / VIUT — the "chief" (старший)
     * duty engineers of the reactor, unit and turbines. These are the colloquial
     * names for {@link #SIUR}, {@link #SIUB} and {@link #SIUT}.
     */
    public enum ControlRoomPost {
        VIUR("ВИУР", "ведущий/старший инженер управления реактором", SIUR),
        VIUB("ВИУБ", "ведущий/старший инженер управления блоком", SIUB),
        VIUT("ВИУТ", "ведущий/старший инженер управления турбинами", SIUT);

        public final String abbr;
        public final String ru;
        public final SkalaRole role;

        ControlRoomPost(String abbr, String ru, SkalaRole role) {
            this.abbr = abbr; this.ru = ru; this.role = role;
        }
    }
}
