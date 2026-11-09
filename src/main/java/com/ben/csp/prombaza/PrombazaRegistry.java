package com.ben.csp.prombaza;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The zone's designated prombazas. Pure state with no Minecraft dependency, so
 * it can be tested directly and serialized by a thin adapter.
 */
public final class PrombazaRegistry {

    private final List<Prombaza> prombazas = new ArrayList<>();

    public Optional<Prombaza> byId(String id) {
        return prombazas.stream().filter(p -> p.id().equals(id)).findFirst();
    }

    public Optional<Prombaza> at(int x, int y, int z) {
        return prombazas.stream().filter(p -> p.contains(x, y, z)).findFirst();
    }

    public List<Prombaza> all() {
        return Collections.unmodifiableList(prombazas);
    }

    public boolean isEmpty() {
        return prombazas.isEmpty();
    }

    /**
     * Designate a new prombaza. Rejects a name or id already in use, and rejects
     * an area that overlaps an existing prombaza — the zone's industrial base is
     * one place, and a second designation over the first is almost always a
     * mistake the director will want to know about rather than have applied.
     */
    public Prombaza designate(String id, String name, Area area, long day, String by) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(area, "area");
        if (byId(id).isPresent()) {
            throw new IllegalStateException("a prombaza with id '" + id + "' already exists");
        }
        for (Prombaza existing : prombazas) {
            if (existing.area().intersects(area)) {
                throw new IllegalStateException("the area overlaps prombaza '"
                    + existing.name() + "' " + existing.area().describe());
            }
        }
        Prombaza p = new Prombaza(id, name, area, day, by);
        prombazas.add(p);
        return p;
    }

    /** Undo a designation. Returns true if a prombaza was removed. */
    public boolean undo(String id) {
        return prombazas.removeIf(p -> p.id().equals(id));
    }

    public void add(Prombaza p) {
        prombazas.add(Objects.requireNonNull(p, "p"));
    }

    public int size() {
        return prombazas.size();
    }
}
