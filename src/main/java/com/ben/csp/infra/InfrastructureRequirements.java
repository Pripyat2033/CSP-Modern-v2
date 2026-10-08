package com.ben.csp.infra;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class InfrastructureRequirements {
    private static final InfrastructureRequirements INSTANCE = new InfrastructureRequirements();
    private Map<String, Map<Integer, List<String>>> requirements;

    private InfrastructureRequirements() {}

    public static InfrastructureRequirements getInstance() {
        return INSTANCE;
    }

    public void loadRequirements(Map<String, Map<Integer, List<String>>> reqs) {
        this.requirements = reqs;
    }

    public List<String> getRequiredBlueprintsForTier(String type, int tier) {
        if (requirements == null || !requirements.containsKey(type)) {
            return Collections.emptyList();
        }
        return requirements.get(type).getOrDefault(tier, Collections.emptyList());
    }
}
