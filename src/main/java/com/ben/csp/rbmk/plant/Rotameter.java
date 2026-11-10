package com.ben.csp.rbmk.plant;

/**
 * A rotameter (ротаметр) — the variable-area flow meter on each of the 1,872 fuel
 * channels. The operator read the individual-channel flow from these; they were
 * how a blockage or a dry channel was spotted. Each rotameter is a separate
 * instrument with its own reading, and the plant tracks them
 * (docs/RBMK-PLANT-SYSTEMS.md §7).
 */
public final class Rotameter {

    /** Number of channel flow meters: one per fuel channel. */
    public static final int COUNT = 1872;
    /** Nominal flow per channel (kg/s): 6 running pumps x 8,000 m3/h / 1,872. */
    public static final double NOMINAL_KGS = 5.34;

    private final int channel;
    private double flowKgS = NOMINAL_KGS;
    /** The float height an operator would see, 0..1 of full scale. */
    private double reading = 0.5;

    public Rotameter(int channel) {
        this.channel = channel;
    }

    public int channel() {
        return channel;
    }

    public double flowKgS() {
        return flowKgS;
    }

    public double reading() {
        return reading;
    }

    /** Set the channel flow (e.g. from the channel's own hydraulics). */
    public void setFlowKgS(double flow) {
        this.flowKgS = Math.max(0, flow);
        this.reading = Math.min(1.0, this.flowKgS / (2 * NOMINAL_KGS));
    }

    /** A channel is starving if its flow has fallen well below nominal. */
    public boolean starving() {
        return flowKgS < 0.5 * NOMINAL_KGS;
    }
}
