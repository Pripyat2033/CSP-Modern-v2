package com.ben.csp.plan;

/**
 * The standard drawing scales of ЕСКД (the unified system of design
 * documentation). A plan is not zoomed continuously; you move between sheets at
 * these fixed scales, each of which carries a different amount of detail.
 * Ordered from the coarsest (most territory) to the most detailed.
 */
public enum Scale {
    S1_2000(2000),
    S1_1000(1000),
    S1_500(500),
    S1_200(200),
    S1_100(100),
    S1_50(50),
    S1_20(20),
    S1_10(10),
    S1_5(5);

    private final int denominator;

    Scale(int denominator) {
        this.denominator = denominator;
    }

    public int denominator() {
        return denominator;
    }

    public String label() {
        return "1:" + denominator;
    }

    /** Zoom in: the next more detailed standard scale, or this one at the limit. */
    public Scale moreDetailed() {
        Scale[] all = values();
        return ordinal() + 1 < all.length ? all[ordinal() + 1] : this;
    }

    /** Zoom out: the next coarser standard scale, or this one at the limit. */
    public Scale lessDetailed() {
        Scale[] all = values();
        return ordinal() - 1 >= 0 ? all[ordinal() - 1] : this;
    }

    public boolean canZoomIn() {
        return ordinal() + 1 < values().length;
    }

    public boolean canZoomOut() {
        return ordinal() - 1 >= 0;
    }
}
