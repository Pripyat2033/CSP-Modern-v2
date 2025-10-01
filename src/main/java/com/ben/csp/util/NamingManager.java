package com.ben.csp.util;

import com.ben.csp.build.EnterpriseType;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A singleton manager for procedurally generating names for enterprises and other objects
 * based on Soviet-era naming conventions.
 */
public class NamingManager {
    private static final NamingManager INSTANCE = new NamingManager();
    private final Random random = new Random();

    // --- Data for name generation ---
    private static final List<String> HONORIFICS = List.of("'Druzhba'", "'Pobeda'", "'Krasny Oktyabr'", "'Leninets'");
    private static final List<String> CONSTRUCTION_SUFFIXES = List.of("Stroy", "Construction Directorate", "Construction Trust", "MontazhUpravlenie");
    private static final List<String> INDUSTRIAL_SUFFIXES = List.of("Industrial Combine", "Factory", "Works", "Zavod");
    private static final List<String> SCIENTIFIC_SUFFIXES = List.of("Scientific Institute", "NII", "Research Complex");
    private static final List<String> LOGISTICS_SUFFIXES = List.of("Avtodor", "Transport Directorate", "Snabzheniye", "Motor-Transport Column");

    // Science Grade: This list is now mutable to track usage and prevent repetition.
    private final List<String> availableGeographicalPrefixes;

    // Counters for numbered facilities
    private final Map<String, Integer> counters = new ConcurrentHashMap<>();

    private NamingManager() {
        // Initialize the list of available prefixes and shuffle it for random-seeming selection.
        this.availableGeographicalPrefixes = new ArrayList<>(List.of("Chernobyl", "Pripyat", "Yanov", "Kopachi", "Poliske"));
        Collections.shuffle(this.availableGeographicalPrefixes, this.random);
    }

    public static NamingManager getInstance() {
        return INSTANCE;
    }

    public String generateEnterpriseName(EnterpriseType type, BlockPos location) {
        String suffix;
        switch (type) {
            case CONSTRUCTION:
                suffix = CONSTRUCTION_SUFFIXES.get(random.nextInt(CONSTRUCTION_SUFFIXES.size()));
                break;
            case INDUSTRIAL:
                suffix = INDUSTRIAL_SUFFIXES.get(random.nextInt(INDUSTRIAL_SUFFIXES.size()));
                break;
            case SCIENTIFIC:
                suffix = SCIENTIFIC_SUFFIXES.get(random.nextInt(SCIENTIFIC_SUFFIXES.size()));
                break;
            case LOGISTICS:
                suffix = LOGISTICS_SUFFIXES.get(random.nextInt(LOGISTICS_SUFFIXES.size()));
                break;
            case DIRECTORATE:
                suffix = "Directorate";
                break;
            default:
                suffix = "Complex";
                break;
        }


        int choice = random.nextInt(10);

        // Science Grade: If we roll for a geographical name but have run out of unique prefixes,
        // gracefully fall back to another naming pattern.
        if (choice >= 4 && choice < 7 && availableGeographicalPrefixes.isEmpty()) {
            choice = random.nextBoolean() ? 0 : 7; // 50/50 chance of numbered or honorific
        }

        if (choice < 4) { // 40% chance of a numbered name
            int number = counters.merge(suffix, 1, Integer::sum);
            return String.format("%s No. %d", suffix, number);
        } else if (choice < 7) { // 30% chance of a geographical name
            String prefix = availableGeographicalPrefixes.remove(0); // Take the next unique prefix from the shuffled list.
            return String.format("%s %s", prefix, suffix);
        } else { // 30% chance of an honorific name
            String honorific = HONORIFICS.get(random.nextInt(HONORIFICS.size()));
            return String.format("%s %s", suffix, honorific);
        }
    }
}