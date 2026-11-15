package com.ben.csp.skala.core;

/**
 * The 24 "functional control groups" of SKALA — the real bottleneck between the
 * processors and the thousands of detectors.
 *
 * <p>A processor cannot hold a wire to every sensor, so when it needs a value it
 * asks the V31M link; the link commands one of the 24 groups, which physically
 * connects the selected sensor line to the link through a commutator; the link
 * converts the analogue signal to a digital value. Each group therefore carries
 * <b>one connection at a time</b>, and the status display shows, per group,
 * whether it is busy, assigned to computer A or B, or restricted by the operator.
 *
 * <p>Known assignments from the source: groups 1-6 watch coolant flow, group 13
 * carries control-rod signals, group 15 drives the mnemonic displays. The rest
 * are not stated and are left unnamed rather than guessed.
 */
public final class FunctionalGroup {

    /** One group's instantaneous state, as shown on the top rows of the panel. */
    public enum State { IDLE, BUSY, COMPUTER_A, COMPUTER_B, RESTRICTED }

    public final int number;       // 1..24
    public final String job;       // what it is known to handle, or "unknown"

    private State state = State.IDLE;

    private FunctionalGroup(int number, String job) {
        this.number = number;
        this.job = job;
    }

    public State state() { return state; }
    public void setState(State s) { this.state = s; }

    /** The 24 groups, with the assignments the sources actually state. */
    public static FunctionalGroup[] all() {
        FunctionalGroup[] g = new FunctionalGroup[24];
        for (int i = 0; i < 24; i++) {
            String job;
            int n = i + 1;
            if (n <= 6) {
                job = "coolant flow (КРВ)";
            } else if (n == 13) {
                job = "control-rod signals";
            } else if (n == 15) {
                job = "mnemonic displays";
            } else {
                job = "unknown (not stated in the sources)";
            }
            g[i] = new FunctionalGroup(n, job);
        }
        return g;
    }

    public static final int COUNT = 24;
}
