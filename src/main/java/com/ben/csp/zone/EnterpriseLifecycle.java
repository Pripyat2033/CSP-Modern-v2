package com.ben.csp.zone;

import com.ben.csp.enterprise.Enterprise;
import com.ben.csp.enterprise.EnterpriseSetup;
import com.ben.csp.enterprise.Facility;
import com.ben.csp.enterprise.Footprint;

import java.util.List;
import java.util.Objects;

/**
 * One enterprise standing itself up, step by step, on the work clock. It builds
 * its HQ, hires its staff, then sets out, builds and has checked each of its
 * facilities in turn — the real order, geodetic set-out before and as-built check
 * after. Nobody drives it but the clock.
 */
public final class EnterpriseLifecycle {

    private enum Phase { HQ, STAFF, FACILITIES, DONE }

    private final Enterprise enterprise;
    private final EnterpriseSetup setup;
    private final List<Footprint> sites;
    private final int staffTarget;

    private Phase phase = Phase.HQ;
    private int facilityIndex = 0;
    private boolean started;

    public EnterpriseLifecycle(Enterprise enterprise, List<Footprint> sites, int staffTarget) {
        this.enterprise = Objects.requireNonNull(enterprise, "enterprise");
        this.setup = new EnterpriseSetup(enterprise.id());
        this.sites = List.copyOf(Objects.requireNonNull(sites, "sites"));
        this.staffTarget = staffTarget;
        if (sites.size() != enterprise.facilities().size()) {
            throw new IllegalArgumentException(
                "the plan must give a plot for every facility of " + enterprise.id());
        }
    }

    public Enterprise enterprise() { return enterprise; }
    public EnterpriseSetup setup() { return setup; }
    public boolean isDone() { return phase == Phase.DONE; }

    /** Advance one step. Returns true if the enterprise has just become operational. */
    public boolean tick(long day) {
        boolean wasOperational = enterprise.isOperational();
        switch (phase) {
            case HQ -> advanceHq();
            case STAFF -> advanceStaff();
            case FACILITIES -> advanceFacility();
            case DONE -> { }
        }
        if (!wasOperational && enterprise.isOperational()) {
            phase = Phase.DONE;
            return true;
        }
        return false;
    }

    private void advanceHq() {
        if (!started) {
            setup.beginHqConstruction();
            started = true;
        }
        setup.advanceHqConstruction(25);
        if (setup.hasHq()) {
            enterprise.assignDirector("director-" + enterprise.id());
            phase = Phase.STAFF;
        }
    }

    private void advanceStaff() {
        if (setup.staffHired() < staffTarget) {
            setup.hire(Math.min(5, staffTarget - setup.staffHired()));
        }
        if (setup.staffHired() >= staffTarget) {
            phase = Phase.FACILITIES;
        }
    }

    private void advanceFacility() {
        List<Facility> facilities = enterprise.facilities();
        if (facilityIndex >= facilities.size()) {
            if (enterprise.isOperational()) {
                phase = Phase.DONE;
            }
            return;
        }
        Facility f = facilities.get(facilityIndex);
        if (!f.hasFootprint()) {
            f.site(sites.get(facilityIndex));
        } else if (!f.isBuilt()) {
            f.completeConstruction();
        } else if (!f.isAsBuiltAccepted() && !f.isAsBuiltRefused()) {
            f.acceptAsBuilt();
        } else if (f.facilityDirectorId() == null) {
            f.assignFacilityDirector("fd-" + f.id());
        } else if (f.staff() < f.requiredStaff()) {
            f.setStaff(f.requiredStaff());
        } else {
            facilityIndex++;
        }
    }

    @Override
    public String toString() {
        return enterprise.id() + " " + phase + " (" + facilityIndex + "/"
            + enterprise.facilities().size() + " facilities)";
    }
}
