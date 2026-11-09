package com.ben.csp.entity.dialogue;

import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;

/**
 * Turns plain chat into speech to the workers. When the Director types a sentence,
 * {@link DialogueEngine} parses it against the world and answers. No command syntax,
 * no keyboard trap: you speak, and they act.
 */
public final class DialogueListener {

    private DialogueListener() {}

    public static void register() {
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            String text = message.getContent().getString();
            if (text.isBlank() || text.startsWith("/")) {
                return;
            }
            DialogueEngine.onChat(sender, text);
        });
    }
}
