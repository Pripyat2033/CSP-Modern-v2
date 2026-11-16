package com.ben.csp.prombaza;

import com.ben.csp.logistics.MaterialBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * The prombaza's own входной контроль desk — the place where the industrial base,
 * not a central authority, enforces compliance on what arrives.
 *
 * <p>This exists to answer one question: why should the enterprise and the
 * prombaza police the deliveries, rather than the whole mod being written to be
 * "accurate" up front? Because that is what the site actually did. Deliveries
 * were routinely defective or off-spec, so each prombaza raised its own workshops
 * and its own receiving desk, stripped every item down and checked it before it
 * went into a building. The compliance check was a physical place, staffed by the
 * workforce, with a cost — roughly half the workforce ended up on it and the
 * project ran about half again as long.
 *
 * <p>So the desk is real state on the prombaza:
 * <ul>
 *   <li>every batch that arrives is run through {@link IncomingControl} and gets a
 *       {@link IncomingControl.Verdict};</li>
 *   <li>only {@code INSTALLED} batches are cleared for the building — a reworked,
 *       returned or quarantined batch does not count as delivered;</li>
 *   <li>the man-hours spent checking accumulate, so the desk can tell the director
 *       what the control is costing in build time.</li>
 * </ul>
 */
public final class PrombazaControl {

    /** One batch received at the desk, with the verdict it was given. */
    public record Receipt(String prombazaId, MaterialBatch batch, IncomingControl.Verdict verdict) {}

    private final String prombazaId;
    private final List<Receipt> receipts = new ArrayList<>();
    private double inspectionManHours;
    private double buildingManHours;
    private long rngSeed = 0x5DEECE66DL;

    public PrombazaControl(String prombazaId) {
        this.prombazaId = Objects.requireNonNull(prombazaId, "prombazaId");
    }

    public String prombazaId() { return prombazaId; }
    public List<Receipt> receipts() { return Collections.unmodifiableList(receipts); }

    /** Man-hours the desk has spent stripping and checking. */
    public double inspectionManHours() { return inspectionManHours; }

    /**
     * Receive a delivery at the desk. The batch is inspected against the load the
     * desk is under (arrivals per inspector, in units of a proper day's work), the
     * verdict is recorded, and the checking cost is booked.
     *
     * @param load arrivals per inspector this period; the desk gets less thorough
     *             as this rises, so a busy site lets more through
     */
    public IncomingControl.Verdict receive(MaterialBatch batch, double load) {
        Objects.requireNonNull(batch, "batch");
        IncomingControl.Verdict verdict = IncomingControl.inspect(batch, load, rng());
        receipts.add(new Receipt(prombazaId, batch, verdict));
        inspectionManHours += verdict.manHours();
        return verdict;
    }

    /** Book man-hours of building work done, so the inspection cost has a base. */
    public void bookBuildingManHours(double hours) {
        buildingManHours += Math.max(0.0, hours);
    }

    /**
     * How much longer the job takes because of the desk: (building + inspection)
     * over building. When the desk has spent half as much effort as the builders,
     * this reads 1.5 — the "50% longer" the site reported.
     */
    public double buildTimeMultiplier() {
        return IncomingControl.buildTimeMultiplier(inspectionManHours, buildingManHours);
    }

    /** How many batches were cleared for the building (INSTALLED only). */
    public int cleared() {
        return (int) receipts.stream()
            .filter(r -> r.verdict().disposition() == IncomingControl.Disposition.INSTALLED)
            .count();
    }

    /** How many batches were found defective — reworked, returned or quarantined. */
    public int held() {
        return receipts.size() - cleared();
    }

    /**
     * How many installed batches are carrying a defect nobody found. These are the
     * dangerous ones: they passed входной контроль and only their behaviour can
     * betray them later — which is what the performance conformance gate is for.
     */
    public int latent() {
        return (int) receipts.stream().filter(r -> r.verdict().latentDefect()).count();
    }

    /** Counts of receipts by disposition, in a stable order. */
    public Map<IncomingControl.Disposition, Integer> byDisposition() {
        Map<IncomingControl.Disposition, Integer> out = new LinkedHashMap<>();
        for (IncomingControl.Disposition d : IncomingControl.Disposition.values()) {
            out.put(d, 0);
        }
        for (Receipt r : receipts) {
            out.merge(r.verdict().disposition(), 1, Integer::sum);
        }
        return out;
    }

    /** One line per batch, for a command or a log. */
    public List<String> report() {
        List<String> lines = new ArrayList<>();
        for (Receipt r : receipts) {
            lines.add(r.batch().materialId + " x" + r.batch().quantity
                + " from " + r.batch().supplierId
                + " -> " + r.verdict().disposition()
                + " (" + r.verdict().defectsFound() + "/" + r.verdict().defectsPresent()
                + " defects found" + (r.verdict().latentDefect() ? ", LATENT" : "") + ")");
        }
        return lines;
    }

    private Random rng() {
        return new Random(rngSeed += 0x9E3779B97F4A7C15L);
    }
}
