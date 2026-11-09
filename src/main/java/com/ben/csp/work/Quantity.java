package com.ben.csp.work;

/**
 * An amount of work in a single {@link WorkUnit}. Arithmetic is only defined
 * between quantities of the same unit; mixing units is a programming error.
 */
public record Quantity(double amount, WorkUnit unit) {

    public Quantity {
        if (unit == null) {
            throw new IllegalArgumentException("unit is required");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("amount must not be negative: " + amount);
        }
    }

    public static Quantity of(double amount, WorkUnit unit) {
        return new Quantity(amount, unit);
    }

    public Quantity plus(Quantity other) {
        requireSameUnit(other);
        return new Quantity(amount + other.amount, unit);
    }

    public Quantity minus(Quantity other) {
        requireSameUnit(other);
        return new Quantity(Math.max(0.0, amount - other.amount), unit);
    }

    public boolean isZero() {
        return amount <= 0.0;
    }

    private void requireSameUnit(Quantity other) {
        if (other.unit != unit) {
            throw new IllegalArgumentException(
                "unit mismatch: " + unit + " vs " + other.unit);
        }
    }

    public String format() {
        return trim(amount) + " " + unit.symbol();
    }

    private static String trim(double v) {
        if (v == Math.rint(v)) {
            return Long.toString((long) v);
        }
        return String.format("%.2f", v);
    }
}
