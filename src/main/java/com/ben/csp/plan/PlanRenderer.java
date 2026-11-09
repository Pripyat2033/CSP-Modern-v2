package com.ben.csp.plan;

import java.util.List;

/**
 * Projects a plan onto a surface of a given size. Implementations decide how a
 * primitive becomes pixels or block faces; the renderer never needs to know what
 * the plan depicts.
 */
public interface PlanRenderer {

    void begin(PlanView view);

    void draw(PlanPrimitive primitive);

    default void drawAll(List<PlanPrimitive> primitives) {
        for (PlanPrimitive p : primitives) {
            draw(p);
        }
    }

    void end();
}
