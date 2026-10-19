package com.ben.csp.entity.ai;

import com.ben.csp.entity.ai.Dosye;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The DPE is responsible for procedurally generating a unique Dosye for each NPC,
 * drawing from data-driven pools of names, places, and traits.
 */
public class DynamicPersonalityEngine {
    private static final DynamicPersonalityEngine INSTANCE = new DynamicPersonalityEngine();

    // These would be loaded from JSON files.
    private final List<String> ukrainianFirstNames = List.of("Anatoly", "Viktor", "Vasily", "Mykola");
    private final List<String> ukrainianLastNames = List.of("Petrenko", "Kovalenko", "Bondarenko");
    private final List<String> russianFirstNames = List.of("Ivan", "Dmitri", "Sergei");
    private final List<String> russianLastNames = List.of("Ivanov", "Petrov", "Sidorov");
    private final List<String> birthplaces = List.of("Kiev, UkSSR", "Moscow, RSFSR", "Kurchatov, KazSSR", "Leningrad, RSFSR");
    private final List<String> nationalities = List.of("Russian", "Ukrainian", "Belarusian", "Kazakh");

    // Science Grade: Added pools for generating more detailed, historically authentic dossiers.
    private final List<String> kharakteristikaTemplates = List.of("Displays ideological maturity.", "A disciplined and reliable worker.", "Sometimes questions directives.", "Shows initiative, perhaps too much.", "Has a politically questionable sense of humor.");
    private final List<String> kompromatEntries = List.of("Uncle lives in America.", "Was reprimanded for lateness in 1982.", "Known to associate with 'unreliable' elements.", "Once made a joke about the General Secretary.");
    private final List<String> pastJobs = List.of("Kolkhoz Tractor Driver", "Factory Welder", "Army Sergeant", "University Researcher");
    private final Random random = new Random();

    private DynamicPersonalityEngine() {}

    /**
     * Get all personality trait options with their descriptions.
     * Uses Dosye.Trait enum directly for meaningful trait access.
     */
    public static List<String> getTraitOptions() {
        return List.of(
            "ANXIOUS - Worried about work safety and quotas",
            "OPTIMISTIC - Believes in collective success",
            "PRAGMATIC - Focuses on results over ideology",
            "DUTIFUL - Always follows orders without question",
            "RECKLESS - Takes risks for personal gain",
            "LOYAL - Dedicated to Party and factory above all"
        );
    }

    public static DynamicPersonalityEngine getInstance() {
        return INSTANCE;
    }

    public Dosye generateDosye() {
        // A simplified generation logic. A real system would have weighted probabilities.
        String firstName;
        String lastName;
        String nationality = nationalities.get(random.nextInt(nationalities.size()));

        if (random.nextBoolean()) {
            firstName = russianFirstNames.get(random.nextInt(russianFirstNames.size()));
            lastName = russianLastNames.get(random.nextInt(russianLastNames.size()));
        } else {
            firstName = ukrainianFirstNames.get(random.nextInt(ukrainianFirstNames.size()));
            lastName = ukrainianLastNames.get(random.nextInt(ukrainianLastNames.size()));
        }

        String patronymic = firstName + "ovich"; // Simplified
        String birthplace = birthplaces.get(random.nextInt(birthplaces.size()));
        Dosye.Trait trait = Dosye.Trait.values()[random.nextInt(Dosye.Trait.values().length)];

        // Generate a more detailed, "Science Grade" dossier.
        Dosye.Kharakteristika kharakteristika = Dosye.Kharakteristika.valueOf(kharakteristikaTemplates.get(random.nextInt(kharakteristikaTemplates.size())).toUpperCase());
        Dosye.PartyStatus partyStatus = Dosye.PartyStatus.values()[random.nextInt(Dosye.PartyStatus.values().length)];

        List<Dosye.WorkRecord> workHistory = new ArrayList<>();
        if (random.nextFloat() < 0.7) { // 70% chance of having a prior job
            workHistory.add(new Dosye.WorkRecord(
                "Previous Employer",
                pastJobs.get(random.nextInt(pastJobs.size())),
                1975 + random.nextInt(10),
                1985
            ));
        }

        List<String> kompromat = new ArrayList<>();
        if (random.nextFloat() < 0.15) { // 15% chance of having compromising material on file.
            kompromat.add(kompromatEntries.get(random.nextInt(kompromatEntries.size())));
        }

        // Create the final dosye object
        Dosye generatedDosye = new Dosye(firstName, patronymic, lastName, nationality, birthplace, trait, workHistory, partyStatus, kharakteristika, kompromat);
        
        // Log creation (for debugging or future use) - uses multiple Dosye getters
        String fullName = generatedDosye.getFullName();
        String nationalityStr = generatedDosye.getNationality();
        String birthplaceStr = generatedDosye.getBirthplace();
        String kharakteristikaStr = "";
        if (generatedDosye.getKharakteristika() != null) {
            kharakteristikaStr = "+ " + generatedDosye.getKharakteristika();
        }
        System.out.println("[DPE] Generated dosye for: " + fullName + " | Nationality: " + nationalityStr + 
                           " | Birthplace: " + birthplaceStr + kharakteristikaStr + " | Party: " + partyStatus);
        
        return generatedDosye;
    }
}