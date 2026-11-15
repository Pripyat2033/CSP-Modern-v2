package com.ben.csp.skala.core;

/**
 * The SKALA status display — the three turquoise electroluminescent panels in the
 * computer hall, and the source of the alarm sound. Reconstructed from the
 * restoration, which rebuilt it from the original blueprints and the operational
 * algorithms.
 *
 * <p>Layout:
 * <ul>
 *   <li><b>Left panel</b> — operation and loss-of-power of the elements of the
 *       reactor control room.</li>
 *   <li><b>Centre panel</b> — the two processors (A and B): their ready / hold /
 *       malfunction state, the frame that glows around the working one, and the
 *       hardware configuration (independent / 1.5 / paired 2.0).</li>
 *   <li><b>Right panel</b> — "restriction of functions": operation and
 *       malfunction of the eight main programs.</li>
 *   <li><b>Top rows</b> — the state of the 24 functional control groups
 *       (busy / computer A / computer B / key / restriction).</li>
 * </ul>
 *
 * <p>On a malfunction the alarm sounds; the operator switches the alarm off,
 * acknowledges, and the blinking stops while the indication stays lit until the
 * fault is cleared. That sequence is modelled exactly.
 */
public final class SkalaStatusDisplay {

    /** The two processors, and how the hardware is allocated between them. */
    public enum ProcessorState { HALTED, READY, RUNNING, HOLD, MALFUNCTION }

    /** Hardware allocation: two singles, 1.5 machines, or a paired 2.0 set. */
    public enum Configuration { INDEPENDENT, ONE_AND_HALF, PAIRED_2_0 }

    private ProcessorState stateA = ProcessorState.HALTED;
    private ProcessorState stateB = ProcessorState.HALTED;
    private Configuration config = Configuration.PAIRED_2_0;

    private boolean alarmSounding;
    private boolean alarmAcknowledged;
    private boolean blinking;

    private final FunctionalGroup[] groups = FunctionalGroup.all();
    private final boolean[] programFaulted = new boolean[SkalaProgram.COUNT];

    public ProcessorState stateA() { return stateA; }
    public ProcessorState stateB() { return stateB; }
    public Configuration configuration() { return config; }
    public FunctionalGroup[] groups() { return groups; }
    public boolean alarmSounding() { return alarmSounding; }
    public boolean blinking() { return blinking; }
    public boolean programFaulted(SkalaProgram p) { return programFaulted[p.ordinal()]; }

    public void setProcessorA(ProcessorState s) { this.stateA = s; }
    public void setProcessorB(ProcessorState s) { this.stateB = s; }
    public void setConfiguration(Configuration c) { this.config = c; }

    /** A processor fault (hardware or software) raises the alarm and blinks. */
    public void raiseMalfunction() {
        alarmSounding = true;
        alarmAcknowledged = false;
        blinking = true;
    }

    /** The operator's first action: silence the alarm (the toggle). */
    public void silenceAlarm() {
        alarmSounding = false;
    }

    /** The operator's second action: acknowledge; blinking stops, indication stays. */
    public void acknowledge() {
        alarmAcknowledged = true;
        blinking = false;
    }

    public void clearMalfunction() {
        alarmSounding = false;
        alarmAcknowledged = false;
        blinking = false;
    }

    /** Mark a program as faulted (lights its frame on the right-hand panel). */
    public void setProgramFault(SkalaProgram p, boolean faulted) {
        programFaulted[p.ordinal()] = faulted;
        if (faulted) {
            raiseMalfunction();
        }
    }

    /** The sound generator's frequency, in Hz — faster rhythm + slower tone. */
    public double alarmHz() {
        return 620.0; // the restoration's two-oscillator alarm sits in this band
    }
}
