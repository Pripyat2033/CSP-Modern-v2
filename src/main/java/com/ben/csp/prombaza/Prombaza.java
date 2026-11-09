package com.ben.csp.prombaza;

import java.util.Objects;

/**
 * A prombaza (промбаза) — the designated industrial base area where the
 * enterprises of the zone build their facilities. It is an area the director
 * marks out on the ground, not a building: within it the enterprises will raise
 * their real plants, offices and yards.
 */
public final class Prombaza {

    private final String id;
    private final String name;
    private final Area area;
    private final long createdDay;
    private final String createdBy;

    public Prombaza(String id, String name, Area area, long createdDay, String createdBy) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.area = Objects.requireNonNull(area, "area");
        this.createdDay = createdDay;
        this.createdBy = Objects.requireNonNull(createdBy, "createdBy");
    }

    public String id() { return id; }
    public String name() { return name; }
    public Area area() { return area; }
    public long createdDay() { return createdDay; }
    public String createdBy() { return createdBy; }

    public boolean contains(int x, int y, int z) {
        return area.contains(x, y, z);
    }

    @Override
    public String toString() {
        return "Prombaza '" + name + "' [" + id + "] " + area.describe();
    }
}
