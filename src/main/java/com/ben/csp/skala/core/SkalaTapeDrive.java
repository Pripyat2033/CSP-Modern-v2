package com.ben.csp.skala.core;

/**
 * The SKALA tape pool: six high-speed reel drives (ES-5070, Zeiss Jena). Each
 * drive has a fixed job in the running system, as described in the Chornobyl
 * Family restoration of the computer hall.
 *
 * <p>The assignment is what made the system survivable and recoverable: the
 * library tape boots the software, the DREG tape is the permanent record (and
 * Unit 4's was recovered after the accident), the snapshot tape is rewritten
 * every day, and PRIZMA keeps its own drive so a long calculation never blocks
 * the operational machine.
 */
public enum SkalaTapeDrive {

    ONE("Drive 1", "Library", "Boot library + main programs; read-only while running",
        true),
    TWO("Drive 2", "PRIZMA", "Dedicated drive for the PRIZMA program", false),
    THREE("Drive 3", "Snapshot / archive restart",
        "Old operating parameters; rewritten every day", false),
    FOUR("Drive 4", "Program buffer",
        "Buffer for programs while their tapes are prepared; hot reserve otherwise",
        false),
    FIVE("Drive 5", "DREG", "The DREG recording tape (the 'black box')", false),
    SIX("Drive 6", "Reserve", "Hot reserve", false);

    public final String label;
    public final String job;
    public final String description;
    /** True if this tape must always be present and is never swapped. */
    public final boolean permanent;

    SkalaTapeDrive(String label, String job, String description, boolean permanent) {
        this.label = label; this.job = job; this.description = description;
        this.permanent = permanent;
    }

    public static final int COUNT = 6;
}
