package com.ben.csp.enterprise;

import java.util.Objects;

/**
 * A real person in the construction of the plant, with the role they held and
 * the source the fact comes from. Nothing here is invented: a person is listed
 * only when a source names them.
 */
public record HistoricalPerson(String id, String fullName, String romanised,
                               String role, String organisation, String years,
                               String source, Provenance provenance) {

    public HistoricalPerson {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(fullName, "fullName");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(provenance, "provenance");
    }

    @Override
    public String toString() {
        return fullName + (romanised != null ? " (" + romanised + ")" : "")
            + " — " + role + (organisation != null ? ", " + organisation : "")
            + (years != null ? ", " + years : "");
    }
}
