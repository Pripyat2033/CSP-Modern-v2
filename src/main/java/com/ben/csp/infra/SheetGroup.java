package com.ben.csp.infra;

/**
 * A group of routes that belongs together on one specialist sheet. A drawing set
 * keeps the site base (boundary and buildings) on every sheet and adds one
 * group's routes to each: roads and rail on the site plan, the utilities on
 * their own network plan.
 */
public enum SheetGroup {
    TRANSPORT("\u0422\u0440\u0430\u043d\u0441\u043f\u043e\u0440\u0442", "Transport"),
    UTILITIES("\u0421\u0435\u0442\u0438", "Utilities");

    private final String russian;
    private final String english;

    SheetGroup(String russian, String english) {
        this.russian = russian;
        this.english = english;
    }

    public String russian() { return russian; }
    public String english() { return english; }
}
