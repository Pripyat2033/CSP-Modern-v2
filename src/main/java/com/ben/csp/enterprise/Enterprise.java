package com.ben.csp.enterprise;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A Soviet specialised enterprise (trust) working the prombaza.
 *
 * <p>Provisional names: the specialities are historical categories, but the
 * exact ChNPP organisation names are not yet verified and are marked as such.
 */
public final class Enterprise {

    private final String id;
    private final String name;
    private final Specialty specialty;
    private final List<FacilityType> requiredFacilities;
    private final Set<FacilityType> builtFacilities = EnumSet.noneOf(FacilityType.class);

    private String directorId;   // assigned Enterprise Director NPC, may be null
    private int personnel;

    public Enterprise(String id, String name, Specialty specialty,
                      List<FacilityType> requiredFacilities) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.specialty = Objects.requireNonNull(specialty, "specialty");
        this.requiredFacilities = List.copyOf(requiredFacilities);
    }

    public String id() { return id; }
    public String name() { return name; }
    public Specialty specialty() { return specialty; }
    public List<FacilityType> requiredFacilities() { return requiredFacilities; }
    public Set<FacilityType> builtFacilities() { return Collections.unmodifiableSet(builtFacilities); }
    public String directorId() { return directorId; }
    public int personnel() { return personnel; }

    public void assignDirector(String directorId) {
        this.directorId = Objects.requireNonNull(directorId, "directorId");
    }

    public void setPersonnel(int personnel) {
        if (personnel < 0) {
            throw new IllegalArgumentException("personnel must not be negative");
        }
        this.personnel = personnel;
    }

    /** Mark a facility as built; only facilities in the required chain count. */
    public void markFacilityBuilt(FacilityType facility) {
        if (!requiredFacilities.contains(facility)) {
            throw new IllegalArgumentException(
                name + " does not require " + facility.label());
        }
        builtFacilities.add(facility);
    }

    public List<FacilityType> missingFacilities() {
        List<FacilityType> missing = new ArrayList<>();
        for (FacilityType f : requiredFacilities) {
            if (!builtFacilities.contains(f)) {
                missing.add(f);
            }
        }
        return missing;
    }

    /** An enterprise can only work once its whole facility chain is up. */
    public boolean isOperational() {
        return builtFacilities.containsAll(requiredFacilities);
    }

    /** How far along the facility chain is, 0..1. */
    public double readiness() {
        if (requiredFacilities.isEmpty()) {
            return 1.0;
        }
        return (double) builtFacilities.size() / requiredFacilities.size();
    }

    @Override
    public String toString() {
        return name + " [" + specialty.label() + "] " + (isOperational() ? "OPERATIONAL"
            : "building (" + builtFacilities.size() + "/" + requiredFacilities.size() + ")");
    }
}
