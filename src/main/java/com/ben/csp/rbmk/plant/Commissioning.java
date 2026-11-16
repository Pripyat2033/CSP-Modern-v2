package com.ben.csp.rbmk.plant;

import java.util.Objects;
import java.util.Random;

/**
 * Commissioning — the start-up test a machine must pass before it is taken into
 * service. This is why a built plant is not born working.
 *
 * <p><b>PLACEHOLDER — THE PROCEDURE IS NOT KNOWN YET.</b> We do not know how a
 * ChNPP unit was actually commissioned in 1986: what was tested, by whom, with
 * what instrument, worked out how, and against what acceptance figure. Ben is
 * clear he has no information on this and neither do we — the "slide rule /
 * mental / Skala" methods below are an <em>illustrative guess</em>, not a claim
 * about the real process, and must not be quoted as one. Before this drives any
 * reactor behaviour it has to be researched against the primary sources (the
 * 1Э-С-II operating regulation we hold, the Unit 4 test programme, Skala's real
 * role). Treat every number and every method here as a stand-in.
 *
 * <p>What the placeholder DOES encode is the one thing that is safe to assert:
 * a machine is commissioned, not declared. A measured figure is never exactly the
 * drawing's figure, and a reading outside the acceptance band fails the test.
 * Two consequences, both of which survive whatever the real procedure turns out
 * to be:
 *
 * <ul>
 *   <li>A machine built from material that passed входной контроль with a defect
 *       nobody found is exactly the machine that fails here — or, just as often,
 *       passes here too and fails in service. Commissioning is a check, not a
 *       guarantee.</li>
 *   <li>Even a <em>good</em> machine can fail its first test if the measurement
 *       is cruder than the band is tight. Nothing comes out of the vault perfect;
 *       some things come out wrong and have to be measured again.</li>
 * </ul>
 */
public final class Commissioning {

    /**
     * How a measurement might have been worked out, and the fractional error the
     * method carries (one-sigma scale, treated here as a symmetric bound). These
     * three are INVENTED EXAMPLES of a spread of precision, not a sourced list of
     * ChNPP's methods — see the class note. Replace with the real methods once the
     * commissioning procedure is researched.
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
