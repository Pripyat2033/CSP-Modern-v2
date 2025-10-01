package com.ben.csp.command;

import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

/**
 * Represents a single, conditional topic of conversation an NPC can have.
 */
public interface DialogueTopic {
    /**
     * A unique identifier for this topic.
     */
    String getId();

    /**
     * The text presented to the player as a choice.
     */
    Text getChoiceText();

    /**
     * Determines if this topic is available for the given NPC.
     * @param npc The NPC to check against.
     * @return true if the topic is available, false otherwise.
     */
    boolean isAvailable(PathAwareEntity npc);

    /**
     * Executes the dialogue when the player chooses this topic.
     * @param player The player who initiated the conversation.
     * @param npc The NPC being spoken to.
     */
    void execute(PlayerEntity player, PathAwareEntity npc);
}