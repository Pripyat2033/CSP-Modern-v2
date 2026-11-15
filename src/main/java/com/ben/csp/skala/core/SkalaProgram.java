package com.ben.csp.skala.core;

/**
 * The main control programs of the pre-1986 SKALA complex.
 *
 * <p>The eight names below are the ones shown on the right-hand panel of the
 * SKALA status display (the "restriction of functions" panel) as reconstructed
 * by the Chornobyl Family restoration. Each program ran on the two V-3M
 * processors and watched one family of the plant. Where a transcript also gave
 * a plain-language description of the program's job, it is recorded here; the
 * programs whose description was not stated are marked unknown rather than
 * guessed.
 *
 * <p>Note on naming: the restoration's episodes transliterate the acronyms
 * inconsistently (e.g. the channel-integrity program appears both as "KTsTK"
 * on the panel and "Katetika" in narration). The panel spelling is used as the
 * key, with the narration variants noted.
 */
public enum SkalaProgram {

    /** КРВ — "Kerva". Monitors coolant consumption (flow) through the active
     *  zone; reads the channel rotameters once a minute. */
    KRV("КРВ", "Kerva", "Coolant-flow monitoring (reads the channel rotameters, 1/min)",
        Period.MINUTE, true),

    /** КЦТК — "KTsTK" (narration: "Katetika"). Channel-integrity control: leaks
     *  inside the active zone, 1/min. */
    KCTK("КЦТК", "KTsTK", "Reactor-channel integrity (leak detectors, 1/min)",
        Period.MINUTE, true),

    /** КИКД — "KIKD" (narration: "Kvad"). Checks that the detectors are
     *  functioning, 1/min. */
    KIKD("КИКД", "KIKD", "Detector function check (1/min)", Period.MINUTE, true),

    /** КИРП — "KIRP". No plain-language description was given in the sources. */
    KIRP("КИРП", "KIRP", "unknown (not described in the sources)", Period.UNKNOWN, false),

    /** ПРИЗМА — the reactor power/physical calculation. Owns tape drive 2. */
    PRIZMA("ПРИЗМА", "PRIZMA",
        "Reactor power / neutron-physical model; reload and ORM calculations",
        Period.PERIODIC, true),

    /** ТАП — "TAP". No plain-language description was given in the sources. */
    TAP("ТАП", "TAP", "unknown (not described in the sources)", Period.UNKNOWN, false),

    /** ДРЕГ — diagnostic registration of parameters; the "black box". Owns drive
     *  5. Its tape of Unit 4 became a primary accident-investigation source. */
    DREG("ДРЕГ", "DREG",
        "Diagnostic registration of parameters (the plant's 'black box')",
        Period.CONTINUOUS, true),

    /** РАО — "RAO" (narration: "Ural"). Registers emergency deviations and
     *  raises alarms. */
    RAO("РАО", "RAO", "Emergency-deviation registration and alarms",
        Period.CONTINUOUS, true);

    /** How often a program scans. */
    public enum Period { MINUTE, PERIODIC, CONTINUOUS, UNKNOWN }

    public final String ru;
    public final String latin;
    public final String description;
    public final Period period;
    public final boolean known;

    SkalaProgram(String ru, String latin, String description, Period period, boolean known) {
        this.ru = ru; this.latin = latin; this.description = description;
        this.period = period; this.known = known;
    }

    public static final int COUNT = 8;
}
