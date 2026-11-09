package com.ben.csp.union;

/**
 * The tier of the Soviet state. Planning and administration ran down a ladder of
 * levels, not from Moscow straight to a building site: the Union, the republic,
 * the oblast, the city, and finally the site itself.
 */
public enum AdministrativeLevel {
    USSR("Союз", "the Union"),
    REPUBLIC("Республика", "the republic (e.g. the Ukrainian SSR)"),
    OBLAST("Область", "the oblast"),
    CITY("Город", "the city"),
    SITE("Площадка", "the site");

    private final String label;
    private final String gloss;

    AdministrativeLevel(String label, String gloss) {
        this.label = label;
        this.gloss = gloss;
    }

    public String label() { return label; }
    public String gloss() { return gloss; }
}
