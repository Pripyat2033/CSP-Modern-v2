package com.ben.csp.planning;

/**
 * Who a person is in the chain of command, and therefore what they may convene.
 *
 * <p>Soviet management rested on единоначалие (one-man command): the chief of a
 * work unit holds full authority over it. So any manager may convene a planerka
 * for their own unit, from the zone director down to an enterprise or facility
 * director. A prorab is an inspector, not a convenor.
 */
public enum Authority {
    DIRECTOR_CHSZO("Director of ChSZO", 100),
    FIRST_DEPUTY("First deputy", 90),
    GLAVNY_INZHENER("Chief engineer", 80),
    ENTERPRISE_DIRECTOR("Enterprise director", 60),
    FACILITY_DIRECTOR("Facility director", 50),
    PRORAB("Prorab (inspector)", 30),
    BRIGADIER("Brigadier", 20),
    WORKER("Worker", 10);

    private final String label;
    private final int rank;

    Authority(String label, int rank) {
        this.label = label;
        this.rank = rank;
    }

    public String label() {
        return label;
    }

    public int rank() {
        return rank;
    }

    /** A manager may convene; a prorab and below may not (they inspect/report). */
    public boolean isManager() {
        return rank >= FACILITY_DIRECTOR.rank;
    }
}
