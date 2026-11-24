package com.ben.csp.radiology;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The loaded set of nuclides REKORD can simulate.
 *
 * <p>Populated by {@link IsotopeLoader} from the sourced data file. An empty
 * registry is a legitimate state (the data file has not been built) but it is
 * never a silent one: {@link IsotopeLoader#problems()} explains any load failure.
 */
public final class IsotopeRegistry {

    private static final IsotopeRegistry INSTANCE = new IsotopeRegistry();
    private final Map<String, Isotope> isotopeMap = new LinkedHashMap<>();

    private IsotopeRegistry() {}

    public static IsotopeRegistry getInstance() {
        return INSTANCE;
    }

    public void loadIsotopes(Map<String, Isotope> loadedIsotopes) {
        isotopeMap.clear();
        isotopeMap.putAll(loadedIsotopes);
    }

    /** Looks up by symbol ("Cs137"). Loads the data file on first use. */
    public Isotope getIsotope(String symbol) {
        if (isotopeMap.isEmpty()) {
            IsotopeLoader.load();
        }
        return isotopeMap.get(symbol);
    }

    public boolean has(String symbol) {
        return getIsotope(symbol) != null;
    }

    public int size() {
        return isotopeMap.size();
    }

    public Collection<Isotope> all() {
        return Collections.unmodifiableCollection(isotopeMap.values());
    }
}
