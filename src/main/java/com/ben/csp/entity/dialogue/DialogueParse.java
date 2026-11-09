package com.ben.csp.entity.dialogue;

import java.util.Set;

/**
 * A spoken sentence parsed into an intent and a target. This is the SHRDLU idea:
 * a small closed vocabulary over a small closed world, so what the Director says is
 * understood by rules, never guessed. It cannot invent an action it does not have.
 *
 * <p>Targets: {@code NEAREST} (the worker you are facing), {@code LAST} (the worker
 * or group last spoken to — the context memory), {@code ALL_NEAR} (everyone in
 * earshot, for "everyone ...").
 */
public record DialogueParse(DialogueIntent intent, Target target) {

    public enum Target { NEAREST, LAST, ALL_NEAR }

    private static final Set<String> GROUP_WORDS =
        Set.of("everyone", "everybody", "all", "brigade", "workers", "comrades", "lads", "team");

    private static final Set<String> LAST_WORDS =
        Set.of("him", "her", "them", "it", "that", "those");

    public static DialogueParse of(String raw) {
        String text = raw == null ? "" : raw.toLowerCase().replaceAll("[^a-z ]", " ").replaceAll("\\s+", " ").trim();
        Target target = Target.NEAREST;
        for (String w : text.split(" ")) {
            if (GROUP_WORDS.contains(w)) {
                target = Target.ALL_NEAR;
                break;
            }
            if (LAST_WORDS.contains(w)) {
                target = Target.LAST;
            }
        }
        return new DialogueParse(DialogueIntent.fromText(raw), target);
    }
}
