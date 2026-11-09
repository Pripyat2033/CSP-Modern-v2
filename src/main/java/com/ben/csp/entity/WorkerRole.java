package com.ben.csp.entity;

/**
 * The roster of ChNPP construction roles, ordered by their place in the
 * chain of command (0 = manual labour ... 7 = enterprise director).
 *
 * <p>The {@link WorkProfile} decides how a worker behaves when it has no
 * active work order: whether it labours, stays at its post, supervises, or
 * roams to survey ground.
 */
public enum WorkerRole {

    TOLKACH("tolkach", 0, WorkProfile.LABOUR),
    STROITEL("stroitel", 0, WorkProfile.LABOUR),
    GEODEZIST("geodezist", 1, WorkProfile.SURVEY),
    UCHETCHIK("uchetchik", 1, WorkProfile.GARRISON),
    KOMMUTATOR("kommutator", 1, WorkProfile.GARRISON),
    BRIGADIER("brigadier", 2, WorkProfile.SUPERVISE),
    SEKRETAR("sekretar", 2, WorkProfile.GARRISON),
    NACHALNIK_SNABZHENIYA("nachalnik_snabzheniya", 2, WorkProfile.GARRISON),
    MASTER("master", 3, WorkProfile.SUPERVISE),
    PRORAB("prorab", 4, WorkProfile.SUPERVISE),
    GLAVNY_INZHENER("glavny_inzhener", 5, WorkProfile.GARRISON),
    FACILITY_DIRECTOR("facility_director", 6, WorkProfile.GARRISON),
    ENTERPRISE_DIRECTOR("enterprise_director", 7, WorkProfile.GARRISON);

    public enum WorkProfile {
        /** Roves around and works at its assigned post. */
        LABOUR,
        /** Stays at its assigned post (offices, exchanges, switchboards). */
        GARRISON,
        /** Patrols and watches over a work area. */
        SUPERVISE,
        /** Ranges widely to measure and stake out ground. */
        SURVEY
    }

    private final String id;
    private final int level;
    private final WorkProfile profile;

    WorkerRole(String id, int level, WorkProfile profile) {
        this.id = id;
        this.level = level;
        this.profile = profile;
    }

    public String getId() {
        return id;
    }

    /** Seniority in the chain of command; higher outranks lower. */
    public int getLevel() {
        return level;
    }

    public WorkProfile getProfile() {
        return profile;
    }

    public String getTranslationKey() {
        return "entity.csp-modern." + id;
    }
}
