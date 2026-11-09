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

    /**
     * Choose the first free plot of the given size, clear of the reserved
     * infrastructure and of everything already taken. Refuses if the territory is
     * full.
     */
    public Footprint allocate(double width, double height) {
        Footprint b = territory.boundary();
        for (double z = b.minZ(); z + height <= b.maxZ(); z += step) {
            for (double x = b.minX(); x + width <= b.maxX(); x += step) {
                Footprint c = Footprint.of(x, z, x + width, z + height);
                if (territory.isReserved(c) || overlapsTaken(c)) {
                    continue;
                }
                taken.add(c);
                return c;
            }
        }
        throw new IllegalStateException(
            "the territory has no free plot " + width + "x" + height + " m");
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
