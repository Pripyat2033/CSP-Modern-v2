package com.ben.csp.plan;

/** How a plan primitive is drawn: a colour and a line width, in pixels. */
public record PlanStyle(int argb, double width) {

    public static PlanStyle of(int argb) {
        return new PlanStyle(argb, 1.0);
    }

    public static PlanStyle of(int argb, double width) {
        return new PlanStyle(argb, width);
    }
}
