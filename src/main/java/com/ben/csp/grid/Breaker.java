package com.ben.csp.grid;

/**
 * A circuit breaker (масляный выключатель, МВ): the thing that makes a real
 * plant switchable. It is not a valve on a pipe — it is a device that can be
 * opened by hand, closed by hand, or opened automatically by protection, and
 * every one of those is a distinct event that the plant must survive or not.
 *
 * <p>A breaker carries a current path, so it sits between a source and a bus.
 * When it opens, whatever was downstream loses its supply with no ramp: this is
 * the discontinuity that separates a controlled shutdown from a blackout.
 */
public final class Breaker {

    /** Why a breaker opened. The cause matters: it is what an operator reads. */
    public enum Trip {
        /** Closed and carrying. */
        NONE,
        /** Opened by the operator. */
        MANUAL,
        /** Opened by protection — overcurrent, reverse power, underfrequency. */
        PROTECTION,
        /** Opened because the upstream source disappeared (the unit tripped). */
        LOSS_OF_SUPPLY
    }

    private final String id;
    private final String name;
    private boolean closed = true;
    private Trip lastTrip = Trip.NONE;

    public Breaker(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String id() { return id; }
    public String name() { return name; }
    public boolean isClosed() { return closed; }
    public Trip lastTrip() { return lastTrip; }

    /** Operator closes the breaker. Refuses if it has been locked out. */
    public void close() {
        closed = true;
        lastTrip = Trip.NONE;
    }

    /** Operator opens the breaker. */
    public void open() {
        closed = false;
        lastTrip = Trip.MANUAL;
    }

    /** Protection opens the breaker. */
    public void tripByProtection() {
        closed = false;
        lastTrip = Trip.PROTECTION;
    }

    /** The supply behind this breaker vanished, so it opened with it. */
    public void loseSupply() {
        closed = false;
        lastTrip = Trip.LOSS_OF_SUPPLY;
    }

    @Override
    public String toString() {
        return name + (closed ? " [closed]" : " [open: " + lastTrip + "]");
    }
}
