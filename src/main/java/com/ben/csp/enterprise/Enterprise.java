package com.ben.csp.enterprise;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A Soviet specialised enterprise (trust) working the zone.
 *
 * <p>An enterprise is not one building: it is a set of real, physical facilities
 * (its headquarters, its office, its production plants) that must each be built,
 * directed and staffed. Only when its whole set is working is the enterprise
 * operational. Until then it can do nothing — it has no plant to do it with.
 */
public final class Enterprise {

    private final String id;
    private final String name;
    private final Specialty specialty;
    private final List<Facility> facilities;

    private String directorId;      // the enterprise director NPC
    private Contract contract;
    private Provenance provenance = Provenance.RECONSTRUCTION;
    private EnterpriseStatus status = EnterpriseStatus.FORMING;

    public Enterprise(String id, String name, Specialty specialty, List<Facility> facilities) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.specialty = Objects.requireNonNull(specialty, "specialty");
        if (facilities.isEmpty()) {
            throw new IllegalArgumentException("an enterprise must own at least one facility");
        }
        this.facilities = List.copyOf(facilities);
    }

    public String id() { return id; }
    public String name() { return name; }
    public Specialty specialty() { return specialty; }
    public List<Facility> facilities() { return Collections.unmodifiableList(facilities); }
    public String directorId() { return directorId; }
    public Contract contract() { return contract; }
    public EnterpriseStatus status() { return status; }

    /** Whether this organisation is a cited fact or a reconstruction. */
    public Provenance provenance() { return provenance; }
    public void setProvenance(Provenance provenance) { this.provenance = java.util.Objects.requireNonNull(provenance); }

    public void assignDirector(String directorId) {
        this.directorId = Objects.requireNonNull(directorId, "directorId");
    }

    public void setContract(Contract contract) {
        this.contract = contract;
    }

    public Facility facility(FacilityType type) {
        return facilities.stream().filter(f -> f.type() == type).findFirst().orElse(null);
    }

    public List<Facility> missingFacilities() {
        List<Facility> missing = new ArrayList<>();
        for (Facility f : facilities) {
            if (!f.isBuilt()) {
                missing.add(f);
            }
        }
        return missing;
    }

    /** Operational once every facility is built, directed and manned. */
    public boolean isOperational() {
        return facilities.stream().allMatch(Facility::isOperational);
    }

    /** How far the build-up has gone, 0..1, counting built facilities. */
    public double readiness() {
        long built = facilities.stream().filter(Facility::isBuilt).count();
        return (double) built / facilities.size();
    }

    /** Advance every facility's production with the clock. */
    public void work(double workHours) {
        for (Facility f : facilities) {
            f.work(workHours);
        }
        refreshStatus();
    }

    private void refreshStatus() {
        if (contract != null && contract.liability() != CriminalLiability.NONE) {
            status = EnterpriseStatus.DELINQUENT;
        } else if (isOperational()) {
            status = EnterpriseStatus.OPERATIONAL;
        } else {
            status = EnterpriseStatus.FORMING;
        }
    }

    @Override
    public String toString() {
        long built = facilities.stream().filter(Facility::isBuilt).count();
        return name + " [" + specialty.label() + "] " + status + " ("
            + built + "/" + facilities.size() + " built)";
    }
}
