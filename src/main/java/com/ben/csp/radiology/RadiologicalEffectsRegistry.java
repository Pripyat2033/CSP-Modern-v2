package com.ben.csp.radiology;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class RadiologicalEffectsRegistry {
    private static final RadiologicalEffectsRegistry INSTANCE = new RadiologicalEffectsRegistry();
    private final List<DoseEffect> effects = new ArrayList<>();

    private RadiologicalEffectsRegistry() {}

    public static RadiologicalEffectsRegistry getInstance() {
        return INSTANCE;
    }

    public void loadEffects(List<DoseEffect> loadedEffects) {
        effects.clear();
        effects.addAll(loadedEffects);
        // Sort by threshold descending, so we can iterate and apply the most severe effects first.
        effects.sort(Comparator.comparingDouble(DoseEffect::thresholdSv).reversed());
    }

    public List<DoseEffect> getApplicableEffects(double totalDose) {
        // Return all effects whose thresholds have been met.
        return effects.stream()
                .filter(effect -> totalDose > effect.thresholdSv())
                .collect(Collectors.toList());
    }
}