package com.ben.csp.radiology;

import java.util.HashMap;
import java.util.Map;

public class SourceTermRegistry {
    private static final SourceTermRegistry INSTANCE = new SourceTermRegistry();
    private final Map<String, SourceTerm> sourceTermMap = new HashMap<>();

    private SourceTermRegistry() {}

    public static SourceTermRegistry getInstance() {
        return INSTANCE;
    }

    public void loadSourceTerms(Map<String, SourceTerm> loadedSourceTerms) {
        sourceTermMap.clear();
        sourceTermMap.putAll(loadedSourceTerms);
    }

    public SourceTerm getSourceTerm(String id) {
        return sourceTermMap.get(id);
    }
}