package com.ben.csp.enterprise;

import java.util.Objects;

/**
 * The standing-up of an enterprise.
 *
 * <p>An enterprise is hired, and it begins by building its headquarters. Until
 * the HQ exists the enterprise director has no office to work from: he cannot
 * hire or dismiss staff, cannot complete design contracts, and cannot plan the
 * location surveys and construction of his facilities. Everything waits on the
 * HQ.
 */
public final class EnterpriseSetup {

    private final String enterpriseId;

    private boolean hqComplete;
    private int hqProgress;          // 0..100, the enterprise builds its own HQ
    private int staffHired;
    private int designContractsCompleted;
    private int level = 1;

    public EnterpriseSetup(String enterpriseId) {
        this.enterpriseId = Objects.requireNonNull(enterpriseId, "enterpriseId");
    }

    public String enterpriseId() { return enterpriseId; }
    public boolean hasHq() { return hqComplete; }
    public int hqProgress() { return hqProgress; }
    public int staffHired() { return staffHired; }
    public int designContractsCompleted() { return designContractsCompleted; }
    public int level() { return level; }

    /** The enterprise begins its own HQ construction as soon as it is hired. */
    public void beginHqConstruction() {
        if (hqComplete) {
            throw new IllegalStateException("the HQ is already built");
        }
    }

    /** Advance HQ construction by a percentage of the work. */
    public void advanceHqConstruction(int percent) {
        if (hqComplete) {
            throw new IllegalStateException("the HQ is already built");
        }
        if (percent < 0) {
            throw new IllegalArgumentException("percent must not be negative");
        }
        hqProgress = Math.min(100, hqProgress + percent);
        if (hqProgress >= 100) {
            hqComplete = true;
        }
    }

    private void requireHq(String what) {
        if (!hqComplete) {
            throw new IllegalStateException(
                "the enterprise has no HQ yet, so it cannot " + what);
        }
    }

    public void hire(int n) {
        requireHq("hire staff");
        if (n < 0) {
            throw new IllegalArgumentException("cannot hire a negative number");
        }
        staffHired += n;
    }

    public void dismiss(int n) {
        requireHq("dismiss staff");
        if (n < 0 || n > staffHired) {
            throw new IllegalArgumentException("invalid number to dismiss");
        }
        staffHired -= n;
    }

    public void completeDesignContract() {
        requireHq("complete design contracts");
        designContractsCompleted++;
    }

    /**
     * Upgrade the enterprise. Needs its HQ, a body of staff, and a record of
     * completed design work — an enterprise cannot grow from nothing.
     */
    public void upgrade() {
        requireHq("upgrade");
        int need = level * 10;
        if (staffHired < need) {
            throw new IllegalStateException("needs at least " + need + " staff to upgrade");
        }
        if (designContractsCompleted < level) {
            throw new IllegalStateException("needs at least " + level + " completed design contracts to upgrade");
        }
        level++;
    }
}
