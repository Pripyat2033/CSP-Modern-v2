package com.ben.csp.zone;

import com.ben.csp.enterprise.Enterprise;
import com.ben.csp.enterprise.EnterpriseSetup;
import com.ben.csp.enterprise.Facility;
import com.ben.csp.enterprise.FacilityType;

import java.util.List;
import java.util.Objects;

/**
 * One enterprise standing itself up, step by step, on the work clock. It builds
 * its HQ, hires its staff, then for each of its facilities CHOOSES a plot inside
 * the prombaza territory, sets it out, builds it and has it checked — the real
 * order, geodetic set-out before and as-built check after.
 *
 * <p>The enterprise is not given a site: it picks one. The territory is laid out
 * first; the enterprise takes a free plot of the size its facility needs, clear of
 * the roads and rail already reserved.
 */
public final class EnterpriseLifecycle {

    private enum Phase { HQ, STAFF, FACILITIES, DONE }

    private final Enterprise enterprise;
    private final EnterpriseSetup setup;
    private final SiteAllocator allocator;
    private final int staffTarget;

    private Phase phase = Phase.HQ;
    private int facilityIndex = 0;
    private boolean started;

    public EnterpriseLifecycle(Enterprise enterprise, SiteAllocator allocator, int staffTarget) {
        this.enterprise = Objects.requireNonNull(enterprise, "enterprise");
        this.setup = new EnterpriseSetup(enterprise.id());
        this.allocator = Objects.requireNonNull(allocator, "allocator");
        this.staffTarget = staffTarget;
    }

    public Enterprise enterprise() { return enterprise; }
    public EnterpriseSetup setup() { return setup; }
    public boolean isDone() { return phase == Phase.DONE; }

    /** The staff an enterprise needs: its facilities' staffs, plus an overhead. */
    public static int staffTargetFor(Enterprise e) {
        return e.facilities().stream().mapToInt(Facility::requiredStaff).sum() + 8;
    }

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
            double[] size = sizeFor(f.type());
            f.site(allocator.allocate(size[0], size[1])); // the enterprise picks its plot
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

    /** The ground a facility of this kind needs, in metres. */
    private static double[] sizeFor(FacilityType type) {
        return switch (type) {
            case HEADQUARTERS -> new double[]{28, 20};
            case CONSTRUCTION_OFFICE -> new double[]{24, 16};
            case STORAGE_YARD -> new double[]{32, 24};
            case WORKSHOP -> new double[]{30, 22};
            case CONCRETE_PLANT -> new double[]{40, 30};
            case REBAR_PLANT -> new double[]{44, 30};
            case EXCAVATION_DEPOT -> new double[]{36, 28};
            case STEEL_YARD -> new double[]{40, 32};
            case CABLE_STORE -> new double[]{24, 18};
            case TRANSPORT_DEPOT -> new double[]{40, 28};
            case PILE_DEPOT -> new double[]{30, 24};
        };
    }

    @Override
    public String toString() {
        return enterprise.id() + " " + phase + " (" + facilityIndex + "/"
            + enterprise.facilities().size() + " facilities)";
    }
}
