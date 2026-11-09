package com.ben.csp.enterprise;

import java.util.Objects;

/**
 * The operating enterprise: the station itself, run by its own director, and the
 * second of the two separate authorities at the site.
 *
 * <p>It is the client the plant is built for and the body that later operates it.
 * It is deliberately NOT the construction administration: the people who build
 * the station and the people who run it are different, and answer up different
 * chains.
 */
public final class OperatingEnterprise {

    private final String id;
    private final String name;
    private final HistoricalPerson director;

    public OperatingEnterprise(String id, String name, HistoricalPerson director) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.director = Objects.requireNonNull(director, "director");
    }

    public String id() { return id; }
    public String name() { return name; }
    public HistoricalPerson director() { return director; }

    @Override
    public String toString() {
        return name + " (director: " + director.fullName() + ")";
    }
}
