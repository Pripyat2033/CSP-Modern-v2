package com.ben.csp.zone;

import com.ben.csp.enterprise.Enterprise;
import com.ben.csp.enterprise.EnterpriseSetup;
import com.ben.csp.enterprise.Facility;
import com.ben.csp.enterprise.FacilityType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * One enterprise standing itself up, step by step, on the work clock. It builds
 * its HQ, hires its staff, then for each of its facilities CHOOSES a plot inside
 * the prombaza territory and BUILDS it over time: the plot is set out as a marker,
 * the construction office assigns a brigade and delivers materials, and the marker
 * becomes a building as the work is done. Only then is it checked as-built,
 * directed and manned.
 */
public final class EnterpriseLifecycle {

    private enum Phase { HQ, STAFF, FACILITIES, DONE }

    private final Enterprise enterprise;
    private final EnterpriseSetup setup;
    private final SiteAllocator allocator;
    private final int staffTarget;
    private final List<Brigade> brigades;
    private final List<ConstructionSite> sites = new ArrayList<>();

    private Phase phase = Phase.HQ;
    private int facilityIndex = 0;
    private boolean started;

    public EnterpriseLifecycle(Enterprise enterprise, SiteAllocator allocator, int staffTarget) {
        this.enterprise = Objects.requireNonNull(enterprise, "enterprise");
        this.setup = new EnterpriseSetup(enterprise.id());
        this.allocator = Objects.requireNonNull(allocator, "allocator");
        this.staffTarget = staffTarget;
        this.brigades = brigadesFor(enterprise);
    }

    public Enterprise enterprise() { return enterprise; }
    public EnterpriseSetup setup() { return setup; }
    public List<Brigade> brigades() { return List.copyOf(brigades); }
    public List<ConstructionSite> sites() { return List.copyOf(sites); }
    public boolean isDone() { return phase == Phase.DONE; }

    /** The staff an enterprise needs: its facilities' staffs, plus an overhead. */
    public static int staffTargetFor(Enterprise e) {
        return e.facilities().stream().mapToInt(Facility::requiredStaff).sum() + 8;
    }

    /** The gangs an enterprise puts on the ground: two, of its own trade strength. */
    private static List<Brigade> brigadesFor(Enterprise e) {
        int totalStaff = e.facilities().stream().mapToInt(Facility::requiredStaff).sum();
        int perBrigade = Math.max(4, totalStaff / 3);
        List<Brigade> out = new ArrayList<>();
        out.add(new Brigade(e.id() + "-br1", e.name() + " brigade 1", e.specialty(), perBrigade, 8.0));
        out.add(new Brigade(e.id() + "-br2", e.name() + " brigade 2", e.specialty(), perBrigade, 8.0));
        return out;
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
            // set out: choose the plot, raise the marker, open the construction site
            double[] size = sizeFor(f.type());
            var plot = allocator.allocate(size[0], size[1]);
            f.site(plot);
            ConstructionSite site = new ConstructionSite(
                "cs-" + f.id(), f.id(), plot, requiredManHours(plot));
            sites.add(site);
            return;
        }

        ConstructionSite site = siteFor(f);
        if (site != null && !site.isComplete()) {
            // the construction office keeps a brigade and materials on the site
            if (!site.hasBrigade()) {
                site.assignBrigade(brigades.get(facilityIndex % brigades.size()).id());
            }
            if (!site.hasMaterials()) {
                site.deliverMaterials(site.materialsRequired() / 4.0);
            }
            Brigade b = brigades.get(facilityIndex % brigades.size());
            site.work(b.dailyManHours());
            return;
        }

        // the building stands: check it, direct it, man it
        if (!f.isBuilt()) {
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

    private ConstructionSite siteFor(Facility f) {
        for (ConstructionSite s : sites) {
            if (s.facilityId().equals(f.id())) {
                return s;
            }
        }
        return null;
    }

    /** Labour a facility of this size needs, in man-hours. Placeholder scale. */
    private static double requiredManHours(com.ben.csp.enterprise.Footprint plot) {
        return plot.area();
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
