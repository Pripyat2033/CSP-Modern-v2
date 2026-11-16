package com.ben.csp.rbmk.plant;

import java.util.Objects;
import java.util.Random;

/**
 * Commissioning — the start-up test a machine must pass before it is taken into
 * service. This is why a built plant is not born working.
 *
 * <p>Equipment was not assumed good because it had been installed. It was tested,
 * by the plant's own engineers, on the plant's own instruments, with the plant's
 * own arithmetic: a slide rule, mental calculation, or the Skala computer where it
 * helped. So the figure that came back was not the figure on the drawing. It was
 * the drawing's figure, plus the error of whoever measured it and however they
 * worked it out.
 *
 * <p>So a machine is commissioned, not declared. Its measured performance is the
 * spec perturbed by the method's error; if that lands outside the acceptance band,
 * the test FAILS and the machine goes back. Two things follow, and both matter:
 *
 * <ul>
 *   <li>A machine built from material that passed входной контроль with a defect
 *       nobody found is exactly the machine that fails here — or, just as often,
 *       passes here too and fails in service. Commissioning is a check, not a
 *       guarantee.</li>
 *   <li>Even a <em>good</em> machine can fail its first test, because the plant's
 *       own crude arithmetic can put a good reading outside a tight band. Nothing
 *       comes out of the vault perfect; some things come out wrong and have to be
 *       measured again.</li>
 * </ul>
 *
 * <p>The error bands and the acceptance tolerance are reconstructions: the
 * mechanism is real, the numbers are not quoted from a source and are marked so.
 */
public final class Commissioning {

    /**
     * How the plant worked a measurement out, and the fractional error the method
     * carries (one-sigma scale, treated here as a symmetric bound). The ordering
     * is the point: the plant's own crude arithmetic is the least precise, the
     * Skala computer the most. RECONSTRUCTION — not sourced.
     */
    public enum Method {
        SLIDE_RULE(0.003, "slide rule"),
        MENTAL(0.03, "mental calculation"),
        SKALA(0.001, "Skala computer");

        private final double error;
        private final String label;

        Method(double error, String label) {
            this.error = error;
            this.label = label;
        }

        /** The fractional error the method adds. */
        public double error() { return error; }

        public String label() { return label; }
    }

    /** The default acceptance band, as a fraction of the expected value. RECONSTRUCTION. */
    public static final double DEFAULT_TOLERANCE = 0.02;

    /**
     * How hard a latent defect of severity s pushes the machine off spec, as a
     * fraction. At 1.0 a severity-0.05 defect moves performance by 5%. RECONSTRUCTION.
     */
    public static final double DEFECT_DEVIATION = 1.0;

    /** The outcome of one commissioning test. */
    public record Result(boolean passed, double expected, double tolerance,
                         double measured, Method method, boolean revealedDefect, String note) {

        /** The reading's error against the drawing, as a fraction. */
        public double error() {
            return expected == 0 ? 0 : (measured - expected) / expected;
        }
    }

    private Commissioning() {}

    /**
     * Commission a machine whose expected performance is {@code expected}, to an
     * acceptance band of {@code tolerance} (fractional), measured by {@code method}.
     *
     * @param latentSeverity the severity of a defect that got through входной
     *        контроль unnoticed, or 0 for none; it biases the machine's true value
     * @param rng the source of the measurement's error
     */
    public static Result commission(double expected, double tolerance, Method method,
                                    double latentSeverity, Random rng) {
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(rng, "rng");
        if (expected <= 0) {
            throw new IllegalArgumentException("expected performance must be positive");
        }
        if (tolerance < 0) {
            throw new IllegalArgumentException("tolerance must not be negative");
        }
        if (latentSeverity < 0) {
            throw new IllegalArgumentException("severity must not be negative");
        }
        boolean hasDefect = latentSeverity > 0;
        double trueValue = expected * (1.0 + (hasDefect ? latentSeverity * DEFECT_DEVIATION : 0.0));
        double err = method.error() * (rng.nextDouble() * 2.0 - 1.0);
        double measured = trueValue * (1.0 + err);
        boolean passed = Math.abs(measured - expected) <= tolerance * expected;
        boolean revealed = hasDefect && !passed;
        String note = passed
            ? "passed"
            : (revealed ? "FAILED — a defect the input check missed" : "FAILED — reading off the band");
        return new Result(passed, expected, tolerance, measured, method, revealed, note);
    }

    /** Commission with the default tolerance. */
    public static Result commission(double expected, Method method, double latentSeverity, Random rng) {
        return commission(expected, DEFAULT_TOLERANCE, method, latentSeverity, rng);
    }
}
