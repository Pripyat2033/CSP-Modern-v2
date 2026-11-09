package com.ben.csp.plan;

import java.util.Optional;

/**
 * Something that can supply a plan for a display to project. A prombaza, a
 * survey, a construction site or a utility network can each be a source; the
 * display does not know or care which.
 */
@FunctionalInterface
public interface PlanSource {

    /** Build the plan, or empty if there is nothing to show yet. */
    Optional<PlanDocument> buildPlan();
}
