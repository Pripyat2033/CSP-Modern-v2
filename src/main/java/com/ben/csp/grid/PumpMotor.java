package com.ben.csp.grid;

import com.ben.csp.rbmk.plant.MainCirculatingPump;

import java.util.Objects;

/**
 * The electric motor of a main circulating pump, as a load on a 6 kV section.
 *
 * <p>This class exists to delete a lie. The pump used to have a {@code running}
 * flag that anything could set, so it could circulate coolant with no electricity
 * anywhere in the plant. Here the motor is a load: it draws 6 MW when its bus is
 * live, and when the bus goes dark the motor loses drive and the pump is left with
 * only its flywheel. The coast-down that follows is not scripted — it is what the
 * pump does when nobody is turning it.
 */
public final class PumpMotor implements PlantLoad {

    private final String id;
    private final MainCirculatingPump pump;
    private final double ratedMw;
    private boolean energised = true;
    /** Set when this motor was the thing that stopped the pump, so it may restart it. */
    private boolean trippedByMotor;

    public PumpMotor(String id, MainCirculatingPump pump, double ratedMw) {
        this.id = Objects.requireNonNull(id, "id");
        this.pump = Objects.requireNonNull(pump, "pump");
        this.ratedMw = ratedMw;
    }

    @Override public String id() { return id; }
    public MainCirculatingPump pump() { return pump; }
    /**
     * A standby pump's motor is isolated from the board — it is stopped and ready
     * to start, not running and drawing — so it contributes nothing to the house
     * load. Three running pumps per circuit is what makes the load 36 MW, not the
     * eight installed pumps.
     */
    @Override public double demandMw() {
        return (energised && !pump.standby()) ? ratedMw : 0;
    }
    /**
     * A standby pump is installed but isolated, so its installed load is zero too:
     * the bus is not sized for a motor that is racked out.
     */
    @Override public double ratedMw() { return pump.standby() ? 0 : ratedMw; }
    @Override public SupplyClass supplyClass() { return SupplyClass.UNIT_AUXILIARY; }
    @Override public boolean isEnergised() { return energised && !pump.standby(); }

    /**
     * The bus changed state. A running pump that loses its motor is tripped into
     * a coast; a pump whose motor comes back restarts if it was not locked out.
     *
     * <p>A standby pump is not affected: its motor is off by design, and losing
     * voltage on a motor that is already stopped is not a new failure.
     */
    @Override
    public void setEnergised(boolean live) {
        boolean wasEnergised = this.energised;
        this.energised = live;
        if (!live) {
            if (pump.running()) {
                pump.trip();
                trippedByMotor = true;
            }
        } else if (!wasEnergised && trippedByMotor && !pump.standby()) {
            // Only a pump this motor stopped may be restarted by it. A pump
            // stopped by a scram or by an operator stays stopped: restoring the
            // voltage does not undo the protection action.
            pump.start();
            trippedByMotor = false;
        }
    }

    @Override
    public String toString() {
        return String.format("%s %.1f MW%s", id, ratedMw, energised ? "" : " [dead]");
    }
}
