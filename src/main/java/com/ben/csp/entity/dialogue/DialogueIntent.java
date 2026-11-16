package com.ben.csp.entity.dialogue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * What a player means when they speak to a worker. This is the first step towards
 * talking to the staff in plain words instead of typing commands: the Director
 * says "come here" and the worker comes.
 *
 * <p>The parse is deliberately simple and deterministic — phrases, not a model.
 * It is easy to extend, and easy to test.
 */
public enum DialogueIntent {

    GREET, COME, FOLLOW, STAND_DOWN, GO_POST, STATUS, WHO, PLACE, HELP, UNKNOWN,

    // The zone and the enterprises. These are spoken to the staff — above all to
    // the secretary — and they run the same operations the operator's commands do.
    FOUND_SITE,       // "found the site" / "start the works" / "lay out the zone"
    SITE_REPORT,      // "how is the site" / "how far have the enterprises come"
    MUSTER,           // "muster the workforce" / "bring the men out"
    PROMBAZA_REPORT,  // "the desk" / "incoming control" / "what has arrived"
    DELIVER;          // "work the wagons" / "clear the siding"

    private static final Set<String> GREETINGS =
        new HashSet<>(Arrays.asList("hello", "hi", "hey", "greetings", "morning", "evening"));

    public static DialogueIntent fromText(String raw) {
        if (raw == null) {
            return UNKNOWN;
        }
        String text = raw.toLowerCase().replaceAll("[^a-z ]", " ").replaceAll("\\s+", " ").trim();
        if (text.isEmpty()) {
            return UNKNOWN;
        }
        Set<String> words = new HashSet<>(Arrays.asList(text.split(" ")));

        // The zone and the enterprises are checked FIRST, so "how is the site" is a
        // site report and not a worker's status.
        if (has(text, "found the site") || has(text, "found the works")
            || has(text, "start the works") || has(text, "lay out the zone")
            || has(text, "stand the enterprises") || has(text, "found the prombaza")
            || has(text, "stand the zone") || has(text, "begin the works")) {
            return FOUND_SITE;
        }
        if (has(text, "how is the site") || has(text, "how is the zone")
            || has(text, "how far have") || has(text, "how is the build")
            || has(text, "report on the zone") || has(text, "the site report")
            || has(text, "how are the enterprises")) {
            return SITE_REPORT;
        }
        if (has(text, "muster") || has(text, "bring the men")
            || has(text, "bring the workforce") || has(text, "call the men out")
            || has(text, "put the men out") || has(text, "everyone out")) {
            return MUSTER;
        }
        if (has(text, "incoming control") || has(text, "the desk")
            || has(text, "what has arrived") || has(text, "any deliveries")
            || has(text, "the inspection")) {
            return PROMBAZA_REPORT;
        }
        if (has(text, "work the wagons") || has(text, "clear the siding")
            || has(text, "the wagons") || has(text, "unload")) {
            return DELIVER;
        }

        if (words.stream().anyMatch(GREETINGS::contains)) {
            return GREET;
        }
        if (has(text, "come here") || has(text, "come to me") || has(text, "come over")
            || words.contains("come")) {
            return COME;
        }
        if (has(text, "follow me") || words.contains("follow")) {
            return FOLLOW;
        }
        if (has(text, "stand down") || has(text, "at ease") || has(text, "wait here")
            || has(text, "stay put") || words.contains("halt") || words.contains("stop")
            || words.contains("stay")) {
            return STAND_DOWN;
        }
        if (has(text, "to your post") || has(text, "to work") || has(text, "get to work")
            || has(text, "go back") || has(text, "carry on") || words.contains("resume")) {
            return GO_POST;
        }
        if (has(text, "what are you doing") || has(text, "status") || has(text, "report")) {
            return STATUS;
        }
        if (has(text, "who are you") || has(text, "your name") || has(text, "introduce")) {
            return WHO;
        }
        if (has(text, "what is this") || has(text, "whats this") || has(text, "where am i")
            || has(text, "this place") || has(text, "what place")) {
            return PLACE;
        }
        if (has(text, "what can you do") || has(text, "orders") || words.contains("help")) {
            return HELP;
        }
        return UNKNOWN;
    }

    private static boolean has(String text, String phrase) {
        return text.contains(phrase);
    }
}
