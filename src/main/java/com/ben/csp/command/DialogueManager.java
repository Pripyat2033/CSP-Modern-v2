package com.ben.csp.command;

import com.google.common.collect.Lists;
import net.minecraft.entity.mob.PathAwareEntity;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages all registered dialogue topics and provides them based on context.
 */
public class DialogueManager {
    private static final DialogueManager INSTANCE = new DialogueManager();
    private final List<DialogueTopic> topics = Lists.newArrayList();

    private DialogueManager() {
        // In the future, dialogue topics will be registered here.
    }

    public static DialogueManager getInstance() {
        return INSTANCE;
    }

    public void registerTopic(DialogueTopic topic) {
        topics.add(topic);
    }

    /**
     * Gets a list of currently available dialogue topics for a given NPC.
     * @param npc The NPC to check topics for.
     * @return A list of available topics.
     */
    public List<DialogueTopic> getAvailableTopics(PathAwareEntity npc) {
        return topics.stream()
                .filter(topic -> topic.isAvailable(npc))
                .collect(Collectors.toList());
    }
}