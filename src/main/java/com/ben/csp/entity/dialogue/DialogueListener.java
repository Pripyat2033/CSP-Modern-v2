package com.ben.csp.entity.dialogue;

import com.ben.csp.entity.CspPersonEntity;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;

import java.util.Comparator;
import java.util.List;

/**
 * Turns plain chat into speech to the workers. When the Director types a sentence,
 * the nearest worker within earshot answers and acts — no command syntax, no
 * keyboard traps, just words. The player's line is left in the chat: it was said
 * aloud.
 */
public final class DialogueListener {

    private DialogueListener() {}

    private static final double EARSHOT = 12.0;

    public static void register() {
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            String text = message.getContent().getString();
            if (text.isBlank() || text.startsWith("/")) {
                return;
            }
            CspPersonEntity worker = nearestWorker(sender);
            if (worker == null) {
                return;
            }
            DialogueIntent intent = DialogueIntent.fromText(text);
            WorkerSpeech.handle(worker, sender, intent);
        });
    }

    private static CspPersonEntity nearestWorker(ServerPlayerEntity player) {
        Box around = player.getBoundingBox().expand(EARSHOT);
        List<CspPersonEntity> near = player.getServerWorld()
            .getEntitiesByClass(CspPersonEntity.class, around, e -> e.isAlive());
        return near.stream()
            .min(Comparator.comparingDouble(player::squaredDistanceTo))
            .orElse(null);
    }
}
