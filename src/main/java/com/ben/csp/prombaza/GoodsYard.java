package com.ben.csp.prombaza;

import com.ben.csp.logistics.MaterialBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The prombaza's rail siding — where deliveries physically arrive, and where the
 * pressure that drives the входной контроль actually comes from.
 *
 * <p>This is what makes the checking LOCAL rather than official. No decree said
 * incoming goods must be inspected. What happened was simpler and harder:
 * defective material backed up the sidings — at Chernobyl the siding was at Yanov
 * station — and a wagon left standing past its norm cost the enterprise money,
 * per wagon, per day (простой вагонов — demurrage). So the site was pulled two
 * ways at once:
 *
 * <ul>
 *   <li><b>clear the yard</b> — every wagon over the norm is a fine, so get them
 *       off the rails fast;</li>
 *   <li><b>don't install junk</b> — the workforce was tired of stripping out
 *       defective material that had already been built in.</li>
 * </ul>
 *
 * <p>The check is what the site did to settle that fight, and this class <em>is</em>
 * the fight. The more wagons stand, the harder the pull to clear them, the less
 * thorough the check — and the more defects reach the building. The check here is
 * not a rule; it is a compromise under a fine.
 *
 * <p>The demurrage figures are a reconstruction. The mechanism — a fine per wagon
 * per day over a free-time norm — is real and documented; the rate is not quoted
 * from a source and is marked so, so it can be replaced with the real tariff.
 */
public final class GoodsYard {

    /** Free time a wagon may stand before demurrage begins, in days. RECONSTRUCTION. */
    public static final double FREE_TIME_DAYS = 2.0;

    /** Fine per wagon per day over the free time. RECONSTRUCTION — not sourced. */
    public static final double FINE_PER_WAGON_DAY = 12.0;

    /** One wagon standing at the siding, with the batch it carries. */
    public static final class Wagon {
        public final String id;
        public final MaterialBatch batch;
        public final long arrivedDay;

        private long clearedDay = -1L;
        private IncomingControl.Disposition outcome;

        Wagon(String id, MaterialBatch batch, long arrivedDay) {
            this.id = id;
            this.batch = batch;
            this.arrivedDay = arrivedDay;
        }

        public boolean isCleared() { return clearedDay >= 0; }
        public long clearedDay() { return clearedDay; }
        public IncomingControl.Disposition outcome() { return outcome; }

        /** Days this wagon has stood: up to today if still standing, to its day if cleared. */
        public double daysStanding(long today) {
            long end = clearedDay >= 0 ? clearedDay : today;
            return Math.max(0, end - arrivedDay);
        }

        /** Demurrage this wagon owes at a given day. */
        public double demurrage(long today) {
            return Math.max(0.0, daysStanding(today) - FREE_TIME_DAYS) * FINE_PER_WAGON_DAY;
        }
    }

    private final String prombazaId;
    private final List<Wagon> wagons = new ArrayList<>();
    private long nextNumber = 1;

    public GoodsYard(String prombazaId) {
        this.prombazaId = Objects.requireNonNull(prombazaId, "prombazaId");
    }

    public String prombazaId() { return prombazaId; }
    public List<Wagon> wagons() { return Collections.unmodifiableList(wagons); }

    /** A loaded wagon arrives at the siding. */
    public Wagon arrive(MaterialBatch batch, long day) {
        Objects.requireNonNull(batch, "batch");
        Wagon w = new Wagon(prombazaId + "-w" + nextNumber++, batch, day);
        wagons.add(w);
        return w;
    }

    /** Wagons still standing (not yet cleared). */
    public List<Wagon> standing() {
        List<Wagon> out = new ArrayList<>();
        for (Wagon w : wagons) {
            if (!w.isCleared()) {
                out.add(w);
            }
        }
        return out;
    }

    public int standingCount() { return standing().size(); }

    /** Total demurrage owed by every wagon at a given day. */
    public double demurrage(long today) {
        double sum = 0;
        for (Wagon w : wagons) {
            sum += w.demurrage(today);
        }
        return sum;
    }

    /**
     * Clear the oldest standing wagon: run it through the desk and take it off the
     * rails. The load the desk is under is the backlog per inspector — the more
     * wagons stand, the more the check is rushed.
     *
     * @return the verdict, or null if nothing is standing
     */
    public IncomingControl.Verdict clearOldest(PrombazaControl desk, long day, double inspectors) {
        Wagon w = null;
        for (Wagon candidate : wagons) {
            if (!candidate.isCleared()) {
                w = candidate;
                break;
            }
        }
        if (w == null) {
            return null;
        }
        double load = standingCount() / Math.max(0.5, inspectors);
        IncomingControl.Verdict v = desk.receive(w.batch, load);
        w.clearedDay = day;
        w.outcome = v.disposition();
        return v;
    }

    /** Clear up to {@code n} standing wagons; returns how many were actually cleared. */
    public int clearUpTo(int n, PrombazaControl desk, long day, double inspectors) {
        int done = 0;
        for (int i = 0; i < n; i++) {
            if (clearOldest(desk, day, inspectors) == null) {
                break;
            }
            done++;
        }
        return done;
    }

    /** Save the yard: every wagon, standing or cleared, with its batch. */
    public net.minecraft.nbt.NbtCompound toNbt() {
        net.minecraft.nbt.NbtCompound nbt = new net.minecraft.nbt.NbtCompound();
        nbt.putString("prombazaId", prombazaId);
        nbt.putLong("next", nextNumber);
        net.minecraft.nbt.NbtList list = new net.minecraft.nbt.NbtList();
        for (Wagon w : wagons) {
            net.minecraft.nbt.NbtCompound c = new net.minecraft.nbt.NbtCompound();
            c.putString("id", w.id);
            c.putLong("arrived", w.arrivedDay);
            c.putLong("cleared", w.clearedDay);
            if (w.outcome != null) {
                c.putString("outcome", w.outcome.name());
            }
            c.put("batch", w.batch.toNbt());
            list.add(c);
        }
        nbt.put("wagons", list);
        return nbt;
    }

    /** Restore a yard from a save. */
    public static GoodsYard fromNbt(net.minecraft.nbt.NbtCompound nbt) {
        GoodsYard y = new GoodsYard(nbt.getString("prombazaId"));
        y.nextNumber = nbt.getLong("next");
        net.minecraft.nbt.NbtList list = nbt.getList("wagons", net.minecraft.nbt.NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            net.minecraft.nbt.NbtCompound c = list.getCompound(i);
            MaterialBatch batch = MaterialBatch.fromNbt(c.getCompound("batch"));
            Wagon w = new Wagon(c.getString("id"), batch, c.getLong("arrived"));
            long cleared = c.getLong("cleared");
            if (cleared >= 0) {
                w.clearedDay = cleared;
            }
            if (c.contains("outcome")) {
                w.outcome = IncomingControl.Disposition.valueOf(c.getString("outcome"));
            }
            y.wagons.add(w);
        }
        return y;
    }

    /** One line per standing wagon, for a command or a log. */
    public List<String> standingReport(long today) {
        List<String> lines = new ArrayList<>();
        for (Wagon w : standing()) {
            lines.add(w.id + ": " + w.batch.materialId + " x" + w.batch.quantity
                + " from " + w.batch.supplierId
                + ", standing " + (int) w.daysStanding(today) + "d"
                + " (fine " + Math.round(w.demurrage(today)) + ")");
        }
        return lines;
    }

    @Override
    public String toString() {
        return "siding[" + prombazaId + "] " + standingCount() + " standing of " + wagons.size();
    }
}
