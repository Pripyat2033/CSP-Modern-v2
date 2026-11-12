package com.ben.csp.rbmk.plant;

/**
 * One room of an energoblok: a sealed, pressure-resistant concrete box.
 *
 * <p>This is the correction the Director made: an RBMK-1000 unit is <em>not</em>
 * one open hall with machines scattered in it. It is a stack of separate,
 * pressure-resistant concrete boxes, and those boxes <b>are the containment</b>.
 * The claim that RBMKs have no containment is false — the containment is these
 * compartments. They are designed for a very different maximum design-basis
 * accident from a Western PWR: not a core melt, but the rupture of one of the
 * large-diameter water pipes feeding the core, which flashes to steam and
 * pressurises the box.
 *
 * <p>Each box therefore has a real design pressure (docs/RBMK-PLANT-SYSTEMS.md §7):
 * the pump rooms are the strongest (0.45 MPa, they hold the pipe ends), the
 * reactor cavity is 0.18 MPa, and the header/inlet rooms are the weakest
 * (0.08 MPa). If a box overpressures beyond its relief, the floor relief
 * assemblies lift and dump steam into the steam-distribution corridor, and — past
 * the relief capacity — the pressure can lift the upper biological shield
 * ("Схема Е", nicknamed "Elena") off its rollers.
 *
 * <p>The box is described by its <b>inner</b> half-open bounds, in block offsets
 * from the energoblok origin (the reactor centre, at the reactor-hall floor). The
 * wall is {@link #wall} blocks thick on all six sides.
 */
public record Compartment(Kind kind, String id,
                          int x0, int y0, int z0,
                          int x1, int y1, int z1,
                          int wall, double designPressurePa) {

    /** The kinds of room in an energoblok, with what they hold and how strong they are. */
    public enum Kind {
        /** The reactor cavity: the reactor itself (Схема Е on top of the core). */
        REACTOR_CAVITY("reactor cavity", 0.18e6),
        /** A main-circulating-pump room: one per circuit, 0.45 MPa (the strongest). */
        PUMP_ROOM("pump room (ГЦН)", 0.45e6),
        /** A separator room: two steam drums per box, left and right. */
        SEPARATOR_ROOM("separator room", 0.18e6),
        /** The header / inlet room: the large-bore pipes. The DBA room, 0.08 MPa. */
        HEADER_ROOM("header room (inlets)", 0.08e6),
        /** The turbine island in the machine hall — not a sealed box. */
        TURBINE_ISLAND("turbine island", 0.0),
        /** The condenser island, under its turbine. */
        CONDENSER_ISLAND("condenser island", 0.0),
        /** The feedwater / deaerator island. */
        FEEDWATER_ISLAND("feedwater island", 0.0);

        private final String label;
        private final double pressurePa;

        Kind(String label, double pressurePa) {
            this.label = label;
            this.pressurePa = pressurePa;
        }

        public String label() {
            return label;
        }

        public double designPressurePa() {
            return pressurePa;
        }
    }

    public Compartment {
        if (x1 <= x0 || y1 <= y0 || z1 <= z0) {
            throw new IllegalArgumentException("compartment " + id + " is empty");
        }
        if (wall < 1) {
            throw new IllegalArgumentException("compartment " + id + " has no wall");
        }
    }

    /** The sealed rooms are the containment; the hall islands are open. */
    public boolean sealed() {
        return kind != Kind.TURBINE_ISLAND
            && kind != Kind.CONDENSER_ISLAND
            && kind != Kind.FEEDWATER_ISLAND;
    }

    /** Is the point (in origin-relative blocks) inside the box's inner volume? */
    public boolean contains(int x, int y, int z) {
        return x >= x0 && x < x1 && y >= y0 && y < y1 && z >= z0 && z < z1;
    }

    /** The inner volume in blocks. */
    public long innerVolume() {
        return (long) (x1 - x0) * (y1 - y0) * (z1 - z0);
    }

    /** Inner size in blocks (x, y, z). */
    public int[] innerSize() {
        return new int[] {x1 - x0, y1 - y0, z1 - z0};
    }

    /** The outer bounds (inner box grown by the wall), half-open. */
    public int[] outerBox() {
        return new int[] {x0 - wall, y0 - wall, z0 - wall,
                          x1 + wall, y1 + wall, z1 + wall};
    }
}
