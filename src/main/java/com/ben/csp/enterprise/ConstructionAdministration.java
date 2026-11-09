package com.ben.csp.enterprise;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The construction administration (управление строительства): the general
 * contractor's line at the site, and one of the two separate authorities there.
 *
 * <p>It is NOT the operating enterprise. The administration builds the plant,
 * headed by its own chief (Kizima), and it holds the specialist trusts that do
 * the work. The station that is built is run by a separate enterprise with its
 * own director. The two lines answer up different chains and are deliberately
 * kept apart.
 */
public final class ConstructionAdministration {

    private final String id;
    private final String name;
    private final HistoricalPerson chief;
    private final List<Enterprise> trusts = new ArrayList<>();

    public ConstructionAdministration(String id, String name, HistoricalPerson chief) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.chief = Objects.requireNonNull(chief, "chief");
    }

    public String id() { return id; }
    public String name() { return name; }
    public HistoricalPerson chief() { return chief; }

    /** Place a trust under the administration. */
    public void addTrust(Enterprise trust) {
        trusts.add(Objects.requireNonNull(trust, "trust"));
    }

    public List<Enterprise> trusts() {
        return Collections.unmodifiableList(trusts);
    }

    public int trustCount() { return trusts.size(); }

    /** Every trust under the administration is operational. */
    public boolean isOperational() {
        return !trusts.isEmpty() && trusts.stream().allMatch(Enterprise::isOperational);
    }

    @Override
    public String toString() {
        return name + " (chief: " + chief.fullName() + "), " + trustCount() + " trusts";
    }
}
