package com.ben.csp.plan;

/**
 * A drawing primitive in plan space (world coordinates, X and Z). A plan is just
 * a set of these on layers; it carries no idea of what it depicts. The same
 * primitives can show a prombaza boundary, a survey, a construction site plan or
 * a utility network.
 */
public sealed interface PlanPrimitive {

    PlanStyle style();

    /** A single point. */
    record Point(double x, double z, PlanStyle style) implements PlanPrimitive {}

    /** A connected run of segments; closed if the last joins the first. */
    record Polyline(double[] xs, double[] zs, boolean closed, PlanStyle style) implements PlanPrimitive {}

    /** An axis-aligned rectangle, outlined or filled. */
    record Rect(double x, double z, double w, double h, boolean filled, PlanStyle style)
        implements PlanPrimitive {}

    /** An arbitrary polygon, outlined or filled. */
    record Polygon(double[] xs, double[] zs, boolean filled, PlanStyle style) implements PlanPrimitive {}

    /** A text label at a point. */
    record Label(double x, double z, String text, PlanStyle style) implements PlanPrimitive {}
}
