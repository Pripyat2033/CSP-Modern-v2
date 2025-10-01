package com.ben.csp.radiology;

import java.util.HashMap;
import java.util.Map;

public class IsotopeRegistry {
    private static final IsotopeRegistry INSTANCE = new IsotopeRegistry();
    private final Map<String, Isotope> isotopeMap = new HashMap<>();

    private IsotopeRegistry() {}

    public static IsotopeRegistry getInstance() {
        return INSTANCE;
    }

    public void loadIsotopes(Map<String, Isotope> loadedIsotopes) {
        isotopeMap.clear();
        isotopeMap.putAll(loadedIsotopes);
    }

    public Isotope getIsotope(String name) {
        return isotopeMap.get(name);
    }
}