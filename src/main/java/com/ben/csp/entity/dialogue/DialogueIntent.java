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

    GREET, COME, FOLLOW, STAND_DOWN, GO_POST, STATUS, WHO, PLACE, HELP, UNKNOWN;

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
