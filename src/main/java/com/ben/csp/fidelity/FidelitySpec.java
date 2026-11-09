package com.ben.csp.fidelity;

import com.ben.csp.enterprise.Footprint;

import java.util.Objects;

/**
 * The designed dimensions of one structure, with the tolerance it must be built
 * to and the drawing it is taken from. This is what "exact scale" means in
 * practice: a structure is not accepted because it looks right, but because its
 * built footprint matches the design within the tolerance the spec allows.
 *
 * <p>The dimensions are entered from the source drawing; nothing here is
 * guessed. A spec with no cited source is not usable.
 */
public record FidelitySpec(String id, String name, double expectedWidth,
                           double expectedLength, double tolerance, String source) {

    public FidelitySpec {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(source, "source");
        if (expectedWidth <= 0 || expectedLength <= 0) {
            throw new IllegalArgumentException("a structure has real dimensions");
        }
        if (tolerance < 0) {
            throw new IllegalArgumentException("tolerance must not be negative");
        }
    }

    /** The error in the X dimension between the design and what was built. */
    public double widthError(Footprint built) {
        return Math.abs(built.width() - expectedWidth);
    }

    /** The error in the Z dimension between the design and what was built. */
    public double lengthError(Footprint built) {
        return Math.abs(built.height() - expectedLength);
    }

    /** The larger of the two dimension errors. */
    public double worstError(Footprint built) {
        return Math.max(widthError(built), lengthError(built));
    }

    /** Whether the built structure is within the spec's tolerance. */
    public boolean isWithinTolerance(Footprint built) {
        return worstError(built) <= tolerance;
    }

    @Override
    public String toString() {
        return name + " " + expectedWidth + "x" + expectedLength + " m"
            + " (tolerance " + tolerance + " m, per " + source + ")";
    }
}
