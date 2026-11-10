package com.ben.csp.zone;

import com.ben.csp.enterprise.Footprint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Hands out plots inside a prombaza territory. An enterprise comes here to choose
 * where its facility goes: it asks for a plot of the size it needs, and gets the
 * first free one, clear of the reserved infrastructure and of anything already
 * taken. The choice is deterministic, so the same territory always yields the
 * same layout.
 */
public final class SiteAllocator {

    private final PrombazaTerritory territory;
    private final double step;
    private final double spacing;
    private final List<Footprint> taken = new ArrayList<>();

    public SiteAllocator(PrombazaTerritory territory) {
        this(territory, 8.0, 4.0);
    }

    public SiteAllocator(PrombazaTerritory territory, double step, double spacing) {
        this.territory = Objects.requireNonNull(territory, "territory");
        this.step = step;
        this.spacing = spacing;
    }

    public PrombazaTerritory territory() { return territory; }

    public List<Footprint> taken() {
        return Collections.unmodifiableList(taken);
    }

    public int takenCount() { return taken.size(); }

    /** Mark a plot as taken, so a restored facility is not built over. */
    public void claim(Footprint plot) {
        taken.add(Objects.requireNonNull(plot, "plot"));
    }

    /**
     * Choose the first free plot of the given size, clear of the reserved
     * infrastructure and of everything already taken. Refuses if the territory is
     * full.
     */
    public Footprint allocate(double width, double height) {
        Footprint b = territory.boundary();
        if (width > b.width() || height > b.height()) {
            throw new IllegalStateException(tooSmall(width, height));
        }
        // Candidate positions are the edges of the territory and the edges of every plot
        // already taken, plus a spacing gap. A plot can always be slid up and left until it
        // meets one of those edges, so trying them finds every fit — including narrow and
        // odd-shaped territories, which a coarse grid would wrongly refuse.
        for (double z : candidates(b.minZ(), b.maxZ(), height)) {
            for (double x : candidates(b.minX(), b.maxX(), width)) {
                Footprint c = Footprint.of(x, z, x + width, z + height);
                if (territory.isReserved(c) || overlapsTaken(c)) {
                    continue;
                }
                taken.add(c);
                return c;
            }
        }
        throw new IllegalStateException(tooSmall(width, height));
    }

    /** Left (or top) edges a plot may take: the territory edge, and each taken plot's
     *  far edge plus a gap, or its near edge less the plot and a gap. */
    private java.util.List<Double> candidates(double lo, double hi, double size) {
        java.util.TreeSet<Double> out = new java.util.TreeSet<>();
        addCandidate(out, lo, lo, hi - size);
        addCandidate(out, hi - size, lo, hi - size);
        for (Footprint t : taken) {
            addCandidate(out, t.maxX() + spacing, lo, hi - size);
            addCandidate(out, t.minX() - size - spacing, lo, hi - size);
            addCandidate(out, t.maxZ() + spacing, lo, hi - size);
            addCandidate(out, t.minZ() - size - spacing, lo, hi - size);
        }
        return new java.util.ArrayList<>(out);
    }

    private static void addCandidate(java.util.TreeSet<Double> out, double v, double lo, double hi) {
        if (v >= lo - 1e-9 && v <= hi + 1e-9) {
            out.add(Math.max(lo, v));
        }
    }

    private String tooSmall(double width, double height) {
        Footprint b = territory.boundary();
        return "the territory (" + (int) b.width() + " x " + (int) b.height()
            + " m) has no free plot " + (int) width + " x " + (int) height
            + " m - the enterprises need more ground than this";
    }

    private boolean overlapsTaken(Footprint c) {
        for (Footprint t : taken) {
            if (withSpacing(c, t)) {
                return true;
            }
        }
        return false;
    }

    private boolean withSpacing(Footprint a, Footprint b) {
        return a.minX() - spacing < b.maxX() && b.minX() - spacing < a.maxX()
            && a.minZ() - spacing < b.maxZ() && b.minZ() - spacing < a.maxZ();
    }
}
