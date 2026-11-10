package com.ben.csp.zone;

import com.ben.csp.enterprise.Enterprise;
import com.ben.csp.enterprise.EnterpriseSetup;
import com.ben.csp.enterprise.Facility;
import com.ben.csp.enterprise.FacilityType;
import com.ben.csp.enterprise.Footprint;

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
    private Footprint hqPlot;
    private Facility hqFacility;
    private boolean awaitingGround;

    public EnterpriseLifecycle(Enterprise enterprise, SiteAllocator allocator, int staffTarget) {
        this.enterprise = Objects.requireNonNull(enterprise, "enterprise");
        this.setup = new EnterpriseSetup(enterprise.id());
        this.allocator = Objects.requireNonNull(allocator, "allocator");
        this.staffTarget = staffTarget;
        this.brigades = brigadesFor(enterprise);
        // The enterprise's own headquarters is one of its facilities; site it now, so the
        // offices are laid out first and can be seen being built. The HQ phase builds it.
        this.hqFacility = findHeadquarters(enterprise);
        siteHqIfPossible();
    }

    public Enterprise enterprise() { return enterprise; }
    public EnterpriseSetup setup() { return setup; }
    public List<Brigade> brigades() { return List.copyOf(brigades); }
    public List<ConstructionSite> sites() { return List.copyOf(sites); }
    public SiteAllocator allocator() { return allocator; }
    public boolean isDone() { return phase == Phase.DONE; }
    /** Where this enterprise's headquarters is being built. */
    public Footprint hqPlot() { return hqPlot; }

    /** The plot the enterprise is working on now: a facility under construction, or the
     *  office while it is being built. The crew is put here. */
    public Footprint activePlot() {
        if (phase == Phase.FACILITIES) {
            for (ConstructionSite s : sites) {
                if (!s.isComplete()) {
                    return s.plot();
                }
            }
        }
        return hqPlot;
    }

    /** A plain phrase for what this enterprise is doing now, for /zone status. */
    public String progressLabel() {
        return switch (phase) {
            case HQ -> "building its office (" + setup.hqProgress() + "%)";
            case STAFF -> "hiring staff (" + setup.staffHired() + "/" + staffTarget + ")";
            case FACILITIES -> awaitingGround ? "waiting for more ground" : "building its facilities";
            case DONE -> "operational";
        };
    }

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

    /** Choose the office's plot, if the territory has room. If it has not, the
     *  enterprise waits: the Director may enlarge the prombaza. */
    private void siteHqIfPossible() {
        if (hqFacility == null || hqPlot != null) {
            return;
        }
        double[] hqSize = sizeFor(FacilityType.HEADQUARTERS);
        try {
            hqPlot = allocator.allocate(hqSize[0], hqSize[1]);
            hqFacility.site(hqPlot);
        } catch (IllegalStateException full) {
            hqPlot = null;
        }
    }

    private void advanceHq() {
        siteHqIfPossible();
        if (hqPlot == null) {
            return;   // no ground yet; try again next day
        }
        if (!started) {
            setup.beginHqConstruction();
            started = true;
        }
        setup.advanceHqConstruction(50);
        if (setup.hasHq()) {
            enterprise.assignDirector("director-" + enterprise.id());
            if (hqFacility != null && !hqFacility.isBuilt()) {
                hqFacility.completeConstruction();
            }
            phase = Phase.STAFF;
        }
    }

    private void advanceStaff() {
        if (setup.staffHired() < staffTarget) {
            setup.hire(Math.min(25, staffTarget - setup.staffHired()));
        }
        if (setup.staffHired() >= staffTarget) {
            phase = Phase.FACILITIES;
        }
    }

    private void advanceFacility() {
        List<Facility> facilities = enterprise.facilities();
        // move past any facility that is already done, so a restored zone and a
        // live one step through the roster in exactly the same way
        while (facilityIndex < facilities.size() && facilities.get(facilityIndex).isOperational()) {
            facilityIndex++;
        }
        if (facilityIndex >= facilities.size()) {
            if (enterprise.isOperational()) {
                phase = Phase.DONE;
            }
            return;
        }
        Facility f = facilities.get(facilityIndex);

        if (!f.hasFootprint()) {
            // set out: choose the plot, raise the marker, open the construction site.
            // If the territory is full, the enterprise waits for more ground rather than
            // failing — the prombaza is what the Director gives it, and it may be enlarged.
            double[] size = sizeFor(f.type());
            Footprint plot;
            try {
                plot = allocator.allocate(size[0], size[1]);
            } catch (IllegalStateException full) {
                awaitingGround = true;
                return;
            }
            awaitingGround = false;
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
        }
        // once the facility is operational, the next tick's loop moves past it
    }

    /** Site a restored facility and reserve its plot, so later builds avoid it. */
    public void claimPlot(Facility f, com.ben.csp.enterprise.Footprint plot) {
        f.site(plot);
        allocator.claim(plot);
        reopenSite(f);
    }

    /** Restore a construction site's progress from a save (by facility order). */
    public void restoreSite(int index, double manHoursDone, double materialsOnSite, String brigadeId) {
        if (index >= 0 && index < sites.size()) {
            sites.get(index).restore(manHoursDone, materialsOnSite, brigadeId);
        }
    }

    /** Open a construction site for a facility being restored, if it has a plot. */
    public void reopenSite(Facility f) {
        // The headquarters is built by the HQ phase, not as a construction site, so it
        // never gets one — including on restore, or it would be built twice.
        if (f.type() == FacilityType.HEADQUARTERS) {
            return;
        }
        if (f.hasFootprint() && siteFor(f) == null) {
            sites.add(new ConstructionSite("cs-" + f.id(), f.id(), f.footprint(),
                requiredManHours(f.footprint())));
        }
    }

    private static Facility findHeadquarters(Enterprise e) {
        for (Facility f : e.facilities()) {
            if (f.type() == FacilityType.HEADQUARTERS) {
                return f;
            }
        }
        return null;
    }

    /** Work out which phase the enterprise is in from its current state. */
    public void recomputePhase() {
        if (enterprise.isOperational()) {
            phase = Phase.DONE;
            return;
        }
        if (!setup.hasHq()) {
            phase = Phase.HQ;
            return;
        }
        if (setup.staffHired() < staffTarget) {
            phase = Phase.STAFF;
            return;
        }
        phase = Phase.FACILITIES;
        List<Facility> facilities = enterprise.facilities();
        facilityIndex = 0;
        for (int i = 0; i < facilities.size(); i++) {
            if (!facilities.get(i).isOperational()) {
                facilityIndex = i;
                return;
            }
        }
    }

    /** The facilities in order, for saving and restoring. */
    public List<Facility> facilities() {
        return enterprise.facilities();
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
