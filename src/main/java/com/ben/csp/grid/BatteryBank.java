package com.ben.csp.grid;

/**
 * The station battery bank and its uninterruptible supply unit (АБП). It feeds
 * the loads that must never see an interruption: protection, instrumentation, the
 * control-rod magnets and the SKALA machine.
 *
 * <p>The distinction that matters is not "battery versus generator" but
 * <b>no-break versus break</b>. The diesels are allowed fifteen seconds of
 * darkness; the batteries are allowed none, because a protection relay that
 * resets is a protection relay that has failed. So this source is modelled as
 * always available until its stored energy is actually spent, and its capacity is
 * in ampere-hours rather than megawatts for that reason.
 */
public final class BatteryBank implements PowerSource {

    /** Nominal DC bus, volts. */
    public static final double NOMINAL_V = 220.0;

    private final String id;
    /** Stored energy, MJ. */
    private final double storedMj;
    private double remainingMj;

    public BatteryBank(String id, double storedMj) {
        this.id = id;
        this.storedMj = storedMj;
        this.remainingMj = storedMj;
    }

    @Override public String id() { return id; }
    public double storedMj() { return storedMj; }
    public double remainingMj() { return remainingMj; }
    public double stateOfCharge() { return storedMj <= 0 ? 0 : remainingMj / storedMj; }
    public boolean isExhausted() { return remainingMj <= 0; }

    /** Recharge from a live board, at the given megawatts for dt seconds. */
    public void charge(double availableMw, double dt) {
        remainingMj = Math.min(storedMj, remainingMj + availableMw * 1e6 * dt / 1e6);
    }

    /** Discharge to carry a load of the given megawatts for dt seconds. */
    public void discharge(double loadMw, double dt) {
        remainingMj = Math.max(0, remainingMj - loadMw * dt);
    }

    /**
     * A battery carries its load until it is flat. Capacity is stated in
     * megawatts for the {@link PowerSource} contract; what actually limits it is
     * {@link #remainingMj()}.
     */
    @Override
    public double availableMw() {
        return isExhausted() ? 0 : Double.MAX_VALUE;
    }

    @Override
    public String toString() {
        return String.format("%s %.0f%% charged", id, 100 * stateOfCharge());
    }
}
