package com.ben.csp.union;

import com.ben.csp.enterprise.Provenance;

import java.util.Objects;

/** One external authority an enterprise answers to, at its level. */
public record Authority(String id, AuthorityKind kind, String name,
                        AdministrativeLevel level, Provenance provenance, String source) {

    public Authority {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(provenance, "provenance");
        Objects.requireNonNull(source, "source");
    }

    @Override
    public String toString() {
        return name + " [" + kind.label() + ", " + level.label() + "]";
    }
}
