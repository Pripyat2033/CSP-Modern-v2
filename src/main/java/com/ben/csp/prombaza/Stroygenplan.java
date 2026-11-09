package com.ben.csp.prombaza;

import com.ben.csp.enterprise.Provenance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A construction site plan (стройгенплан): the drawing of the temporary works
 * that serve a building site — offices, records archives, warehouses, canteens,
 * workshops, railheads, truck parks, crane tracks, site roads and gates.
 *
 * <p>This is the honest answer to "we do not know the prombaza's layout". The
 * one fact that is known is its position; every facility on the plan is a
 * RECONSTRUCTION unless it is individually marked otherwise. The plan therefore
 * says what it is: a provisional layout, not a record.
 */
public final class Stroygenplan {

    private final String prombazaId;
    private final String positionNote;     // the known fact about where it stood
    private final Provenance positionProvenance;
    private final String positionSource;
    private final List<SiteFacility> facilities = new ArrayList<>();

    public Stroygenplan(String prombazaId, String positionNote,
                        Provenance positionProvenance, String positionSource) {
        this.prombazaId = Objects.requireNonNull(prombazaId, "prombazaId");
        this.positionNote = Objects.requireNonNull(positionNote, "positionNote");
        this.positionProvenance = Objects.requireNonNull(positionProvenance, "positionProvenance");
        this.positionSource = Objects.requireNonNull(positionSource, "positionSource");
    }

    public String prombazaId() { return prombazaId; }
    public String positionNote() { return positionNote; }
    public Provenance positionProvenance() { return positionProvenance; }
    public String positionSource() { return positionSource; }

    /** Add a facility to the plan; new facilities are reconstructions by default. */
    public Stroygenplan add(String name, SiteFacilityKind kind) {
        facilities.add(new SiteFacility(name, kind, Provenance.RECONSTRUCTION));
        return this;
    }

    /** Add a facility that is known from a source. */
    public Stroygenplan addVerified(String name, SiteFacilityKind kind) {
        facilities.add(new SiteFacility(name, kind, Provenance.VERIFIED));
        return this;
    }

    public List<SiteFacility> facilities() {
        return Collections.unmodifiableList(facilities);
    }

    public int size() { return facilities.size(); }

    public long verifiedCount() {
        return facilities.stream().filter(f -> f.provenance().isVerified()).count();
    }

    /** True when nothing on the plan is claimed as history. */
    public boolean isAllReconstruction() {
        return facilities.stream().noneMatch(f -> f.provenance().isVerified());
    }

    @Override
    public String toString() {
        return "Стройгенплан of " + prombazaId + ": " + size() + " facilities, "
            + verifiedCount() + " verified; position " + positionProvenance.label()
            + " (" + positionNote + ")";
    }
}
