package com.ben.csp.prombaza;

import com.ben.csp.enterprise.Provenance;

import java.util.Objects;

/** One facility drawn on a construction site plan, with how it is known. */
public record SiteFacility(String name, SiteFacilityKind kind, Provenance provenance) {

    public SiteFacility {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(provenance, "provenance");
    }

    @Override
    public String toString() {
        return name + " (" + kind.label() + ") [" + provenance.label() + "]";
    }
}
