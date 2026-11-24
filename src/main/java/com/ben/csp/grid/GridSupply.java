package com.ben.csp.grid;

/**
 * The national grid as seen from the plant's switchyard: a very large source that
 * is either there or not. It is also the reserve supply — the РТСН takes its
 * power from here — which is why losing the grid while the unit is tripped is a
 * station blackout rather than an inconvenience.
 *
 * <p>Import capacity is limited by the switchyard ties, not by the grid, so the
 * figure here is the plant's import limit and it is marked to verify.
 */
public final class GridSupply implements PowerSource {

    private final String id;
    /** Maximum import through the switchyard ties, MW. */
    private final double importLimitMw;
    private boolean available = true;

    public GridSupply(String id, double importLimitMw) {
        this.id = id;
        this.importLimitMw = importLimitMw;
    }

    @Override public String id() { return id; }
    public double importLimitMw() { return importLimitMw; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    @Override
    public double availableMw() {
        return available ? importLimitMw : 0;
    }

    @Override
    public String toString() {
        return "grid " + (available ? "available" : "LOST")
            + " (import limit " + importLimitMw + " MW)";
    }
}
