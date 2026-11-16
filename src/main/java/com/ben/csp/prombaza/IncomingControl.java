package com.ben.csp.prombaza;

import com.ben.csp.logistics.Defect;
import com.ben.csp.logistics.MaterialBatch;

import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Входной контроль — incoming inspection at the prombaza. This is where the
 * enterprise, not a ministry and not a global rule, enforces compliance.
 *
 * <p>Be careful not to read this as an official procedure. No decree from above
 * said incoming goods must be checked. What happened was local, selfish, and
 * practical. Defective material backed up the sidings — at Chernobyl, the siding
 * at Yanov station — and a wagon left standing past its norm was a fine the
 * enterprise paid, per wagon, per day (простой вагонов). Meanwhile the workforce
 * was sick of stripping out defective material that had already been built in.
 * So the site was pulled both ways — clear the yard to stop the fines, but do not
 * install junk — and the check was the compromise it settled on. See
 * {@link GoodsYard} for the pressure that drives it.
 *
 * <p>The cost was enormous: roughly half the workforce ended up on the receiving
 * dock, disassembling and checking, and the project ran correspondingly longer.
 * That is the whole reason a compliance gate belongs with the ACTOR who receives
 * the goods — it is their yard, their fine, and their rework.
 *
 * <p>This class is that gate, and it is deliberately NOT a pass/fail against a
 * perfect inspector. The point of the history is that the inspection was real but
 * imperfect, and that its thoroughness fell as the volume of material rose. So it
 * models three things at once:
 * <ul>
 *   <li>detection — a defect is found with a probability set by how easy it is to
 *       see ({@link Defect#discoverability()}) and how thorough the inspector is;</li>
 *   <li>thoroughness — the more material per inspector, the less thorough the
 *       check, so defects slip through exactly when the site is busiest;</li>
 *   <li>cost — the man-hours spent inspecting are man-hours not spent building,
 *       which is the "50% longer" the site reported.</li>
 * </ul>
 *
 * <p>The disposition is the one the workforce used: a clean item is
 * {@link Disposition#INSTALLED}; an item with a defect that was FOUND goes to the
 * on-site workshop to be reworked ({@link Disposition#REWORKED}) or back to the
 * supplier if it is too severe to fix ({@link Disposition#RETURNED}); an item
 * that is too suspect to trust is held ({@link Disposition#QUARANTINED}). The
 * dangerous case is a defect that was NOT found: the item is INSTALLED, but it
 * carries a latent defect — and that is the case the performance conformance
 * check ({@code plant.MachineryRegister}) exists to catch later, by its effect on
 * behaviour.
 */
public final class IncomingControl {

    /** Where a delivered item ends up after входной контроль. */
    public enum Disposition {
        /** Checked and passed; it may go into the building. */
        INSTALLED,
        /** A defect was found; the on-site workshop will fix it. */
        REWORKED,
        /** A defect was found that is too severe to fix here; back to the supplier. */
        RETURNED,
        /** No defect proven, but the batch is too suspect to trust; hold it. */
        QUARANTINED
    }

    /** One inspection decision for one batch. */
    public record Verdict(Disposition disposition, int defectsPresent, int defectsFound,
                          boolean latentDefect, double manHours) {
        /** True only when the batch was installed with nothing left hiding. */
        public boolean passedClean() {
            return disposition == Disposition.INSTALLED && !latentDefect;
        }
    }

    /** Hours of stripping and checking per unit of material, at full thoroughness. */
    public static final double HOURS_PER_UNIT = 0.5;

    /** A defect at or above this severity cannot be fixed on site; it goes back. */
    public static final double SEVERE = 0.6;

    /** Below this inherent quality, a batch with no proven defect is still held. */
    public static final double SUSPECT_QUALITY = 0.5;

    /** The floor on thoroughness — an inspector never stops looking entirely. */
    public static final double MIN_THOROUGHNESS = 0.25;

    private IncomingControl() {}

    /**
     * How thorough the check is, given the load. Load is the material arriving per
     * inspector per day, as a multiple of what one inspector can check properly.
     * At load 0 the check is full (1.0); it falls off as load rises, with a floor,
     * because the workforce that should be building is instead on the receiving
     * dock — the "50% longer".
     */
    public static double thoroughness(double load) {
        double l = Math.max(0.0, load);
        return Math.max(MIN_THOROUGHNESS, 1.0 / (1.0 + l));
    }

    /**
     * Inspect a batch. Each defect is found with a probability set by how easy it
     * is to see and how thorough the check is; a found defect is recorded on the
     * batch ({@link MaterialBatch#markDefectAsFound}). The disposition follows the
     * workforce's rule. A defect that is NOT found leaves the batch INSTALLED but
     * carrying a latent defect — the case only behaviour can catch later.
     *
     * @param load the arrival rate per inspector, in units of a proper day's work
     * @param rng  the source of the inspection's luck (injected so it is testable)
     */
    public static Verdict inspect(MaterialBatch batch, double load, Random rng) {
        Objects.requireNonNull(batch, "batch");
        Objects.requireNonNull(rng, "rng");
        double thorough = thoroughness(load);

        List<Defect> defects = batch.getDefects();
        int present = defects.size();
        int found = 0;
        double worstFound = 0.0;
        for (Defect d : defects) {
            double p = clamp01(d.discoverability() * thorough);
            if (rng.nextDouble() < p) {
                batch.markDefectAsFound(d);
                found++;
                worstFound = Math.max(worstFound, d.severity());
            }
        }
        boolean latent = found < present;

        Disposition disposition;
        if (found > 0 && worstFound >= SEVERE) {
            disposition = Disposition.RETURNED;
        } else if (found > 0) {
            disposition = Disposition.REWORKED;
        } else if (batch.inherentQuality < SUSPECT_QUALITY) {
            disposition = Disposition.QUARANTINED;
        } else {
            disposition = Disposition.INSTALLED;
        }

        double manHours = batch.quantity * HOURS_PER_UNIT * thorough;
        return new Verdict(disposition, present, found, latent, manHours);
    }

    /**
     * The build-time multiplier caused by diverting the workforce to inspection.
     * If the inspectors spend {@code inspectionManHours} while the builders have
     * {@code buildingManHours}, the job takes (building + inspection)/building
     * times as long. When inspection is half the building effort, this is 1.5 —
     * the "50% longer" the site reported.
     */
    public static double buildTimeMultiplier(double inspectionManHours, double buildingManHours) {
        if (buildingManHours <= 0) {
            return 1.0;
        }
        return 1.0 + Math.max(0.0, inspectionManHours) / buildingManHours;
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
