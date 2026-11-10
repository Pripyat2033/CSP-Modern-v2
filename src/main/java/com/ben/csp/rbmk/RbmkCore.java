package com.ben.csp.rbmk;

import java.util.List;

/**
 * The RBMK-1000 core as a SIMULATION, decoupled from Minecraft's block grid.
 *
 * <p>The core is not a pile of blocks. It is 1,661 fuel channels and 211 control
 * channels, each a column of axial nodes, each node carrying its own material,
 * neutron flux, coolant void and temperature. That is the "1,661 small reactors",
 * and it is <em>data</em>, not geometry — so its granularity does not depend on how
 * many blocks the world spends on it.
 *
 * <p>This is the answer to the scale problem. The world can show the core at any
 * size, or as a machine with a detailed model, while the simulation runs at full
 * per-channel, per-node resolution regardless. The tick cost — the only thing that
 * decides whether Minecraft can carry the plant — is the cost of stepping THIS
 * array, and that is independent of block scale.
 */
public final class RbmkCore {

    /** Axial mesh of the core: the solver's nodes per channel. */
    public static final int AXIAL_NODES = 24;

    /** What fills one axial node of a channel. */
    public enum Material { GRAPHITE, WATER, FUEL, B4C, STEEL }

    private final List<RbmkLattice.Channel> channels;
    private final byte[] material;      // AXIAL_NODES per channel
    private final double[] fluxFast;
    private final double[] fluxThermal;
    private final double[] coolantVoid;
    private final double[] temperature;

    public RbmkCore() {
        this.channels = RbmkLattice.channels();
        int n = channels.size() * AXIAL_NODES;
        this.material = new byte[n];
        this.fluxFast = new double[n];
        this.fluxThermal = new double[n];
        this.coolantVoid = new double[n];
        this.temperature = new double[n];
        load();
    }

    private void load() {
        for (int c = 0; c < channels.size(); c++) {
            boolean control = channels.get(c).control();
            for (int z = 0; z < AXIAL_NODES; z++) {
                int i = index(c, z);
                material[i] = (byte) (control ? Material.WATER : Material.FUEL).ordinal();
            }
        }
    }

    public int channelCount() {
        return channels.size();
    }

    public int nodeCount() {
        return material.length;
    }

    public int index(int channel, int node) {
        return channel * AXIAL_NODES + node;
    }

    public Material materialAt(int channel, int node) {
        return Material.values()[material[index(channel, node)]];
    }

    public void setMaterialAt(int channel, int node, Material m) {
        material[index(channel, node)] = (byte) m.ordinal();
    }

    public double thermalFluxAt(int channel, int node) {
        return fluxThermal[index(channel, node)];
    }

    public void setThermalFluxAt(int channel, int node, double v) {
        fluxThermal[index(channel, node)] = v;
    }

    public boolean isControl(int channel) {
        return channels.get(channel).control();
    }

    /** Rough resident size of the core state, in bytes. */
    public long memoryBytes() {
        int n = nodeCount();
        return (long) n * (1 + 8 * 4); // one byte + four doubles per node
    }
}
