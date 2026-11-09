package com.ben.csp.union;

import com.ben.csp.enterprise.Provenance;

import java.util.Objects;

/**
 * A real link between the zone and the rest of the country: a railhead, a road,
 * a river port. Beyond these nodes the country is abstracted — the node is where
 * goods and people cross the boundary.
 */
public record TransportNode(String id, String name, TransportKind kind,
                            String connectsTo, Provenance provenance, String source) {

    public TransportNode {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(provenance, "provenance");
        Objects.requireNonNull(source, "source");
    }

    @Override
    public String toString() {
        return name + " (" + kind.label() + " to " + connectsTo + ")";
    }
}
