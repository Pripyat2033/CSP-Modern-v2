package com.ben.csp.rbmk.plant;

/**
 * A valve on the main circuit — a gate, check or control valve. Each is its own
 * machine with its own position: a gate valve isolates a line, a control valve
 * throttles flow, and a check valve only lets flow one way. The RBMK circuit had
 * hundreds of them (docs/RBMK-PLANT-SYSTEMS.md §2).
 */
public final class Valve {

    /** What the valve does. */
    public enum Kind { GATE, CONTROL, CHECK }

    private final Kind kind;
    /** Position 0 (closed) .. 1 (open). */
    private double position = 1.0;
    /** Target position for a control valve. */
    private double target = 1.0;

    public Valve(Kind kind) {
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    public double position() {
        return position;
    }

    public boolean open() {
        return position > 0.99;
    }

    public boolean closed() {
        return position < 0.01;
    }

    /** Drive a control valve to a position, or open/close a gate valve. */
    public void setPosition(double p) {
        this.target = Math.max(0, Math.min(1, p));
        if (kind == Kind.GATE) {
            this.position = this.target; // gate valves actuate fully
        }
    }

    /** Advance a control valve toward its target over ~2 s. */
    public void step(double dt) {
        if (kind == Kind.GATE) {
            return;
        }
        position += (target - position) * Math.min(1.0, dt / 2.0);
    }

    /** Fraction of the flow this valve passes. A check valve can be one-way only. */
    public double flowFactor() {
        return position;
    }
}
