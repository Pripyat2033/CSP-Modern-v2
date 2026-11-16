package com.ben.csp.zone;

import com.ben.csp.logistics.MaterialBatch;
import com.ben.csp.prombaza.GoodsYard;
import com.ben.csp.prombaza.IncomingControl;
import com.ben.csp.prombaza.PrombazaControl;

import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * The seam between a delivery and the building: material arrives at whatever
 * quality the supplier sent, goes through the prombaza's входной контроль, and
 * only what comes out cleared reaches the site.
 *
 * <p>This is the piece that stops the mod from building a perfect plant. Material
 * does not arrive good. It arrives from several suppliers at a spread of
 * qualities — some honest, some not — and a batch is good or bad on the day. The
 * site asks for a quantity; what it <em>gets</em> is whatever survived the check.
 * Defective material that was caught goes back or into the workshop, and the site
 * is short; material that was not caught goes in and carries its defect into the
 * building. Either way the plan and the reality are not the same thing.
 *
 * <p>The supplier quality spread is a reconstruction. The mechanism — variable
 * incoming quality, a check that costs time and is not perfect — is real; the
 * numbers are not quoted and are marked so.
 */
public final class MaterialSupply {

    /** One supplier, with the mean quality of what it sends. RECONSTRUCTION. */
    public record Supplier(String id, double meanQuality) {}

    /** The default supplier spread: one good, one fair, one poor. RECONSTRUCTION. */
    public static final List<Supplier> DEFAULT_SUPPLIERS = List.of(
        new Supplier("zavod-A", 0.95),
        new Supplier("zavod-B", 0.80),
        new Supplier("artel-V", 0.62));

    /** How much material one wagon carries, in site units. RECONSTRUCTION. */
    public static final double UNITS_PER_WAGON = 20.0;

    private final GoodsYard yard;
    private final PrombazaControl desk;
    private final List<Supplier> suppliers;
    private long rngSeed = 0x2545F4914F6CDD1DL;

    public MaterialSupply(GoodsYard yard, PrombazaControl desk, List<Supplier> suppliers) {
        this.yard = Objects.requireNonNull(yard, "yard");
        this.desk = Objects.requireNonNull(desk, "desk");
        this.suppliers = List.copyOf(Objects.requireNonNull(suppliers, "suppliers"));
        if (suppliers.isEmpty()) {
            throw new IllegalArgumentException("at least one supplier is required");
        }
    }

    public MaterialSupply(GoodsYard yard, PrombazaControl desk) {
        this(yard, desk, DEFAULT_SUPPLIERS);
    }

    /** What a delivery attempt actually achieved. */
    public record Delivery(double requested, double delivered, double lost, int wagons,
                           int held) {
        /** The fraction of the request that reached the site. */
        public double fulfilment() {
            return requested == 0 ? 1.0 : delivered / requested;
        }
    }

    /**
     * Try to deliver {@code amount} of material to a site. The material arrives as
     * wagons from the suppliers, at their own variable quality; the yard's backlog
     * sets how rushed the check is; and the site receives only what was cleared.
     *
     * <p>Reworked material still reaches the site — the workshop fixed it. Returned
     * and quarantined material does not, so a bad supplier simply shortens the
     * delivery. The site is not told in advance; it finds out by being short.
     */
    public Delivery deliverTo(ConstructionSite site, double amount, long day, double inspectors) {
        Objects.requireNonNull(site, "site");
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        Random rng = rng();
        // The train arrives first: every wagon is set on the siding before the desk
        // touches one. That is what makes the backlog real, and the backlog is what
        // rushes the check.
        java.util.List<GoodsYard.Wagon> arrived = new java.util.ArrayList<>();
        double remaining = amount;
        while (remaining > 1e-9) {
            double chunk = Math.min(remaining, UNITS_PER_WAGON);
            Supplier s = suppliers.get(rng.nextInt(suppliers.size()));
            double quality = clamp01(s.meanQuality() + (rng.nextDouble() - 0.5) * 0.4);
            MaterialBatch batch = new MaterialBatch(
                site.facilityId(), (int) Math.max(1, Math.round(chunk)), quality, s.id());
            arrived.add(yard.arrive(batch, day));
            remaining -= chunk;
        }
        // Then the desk works through them, oldest first, as fast as it can.
        double delivered = 0;
        int held = 0;
        for (GoodsYard.Wagon w : arrived) {
            IncomingControl.Verdict v = yard.clearOldest(desk, day, inspectors);
            if (v == null) {
                break;
            }
            switch (v.disposition()) {
                // Reworked material was fixed in the on-site workshop, so it still
                // reaches the site. Returned and quarantined material does not.
                case INSTALLED, REWORKED -> delivered += w.batch.quantity;
                default -> held++;
            }
        }
        double lost = Math.max(0.0, amount - delivered);
        return new Delivery(amount, delivered, lost, arrived.size(), held);
    }

    private Random rng() {
        return new Random(rngSeed += 0x9E3779B97F4A7C15L);
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
