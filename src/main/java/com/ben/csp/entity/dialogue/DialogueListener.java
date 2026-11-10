package com.ben.csp.entity.dialogue;

import com.ben.csp.entity.CspPersonEntity;
import com.ben.csp.telephony.KommutatorExchange;
import com.ben.csp.telephony.TelephonyState;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Turns plain chat into speech. When the Director types a sentence, the workers
 * within earshot answer and act. If instead the Director is holding a call, the words
 * go down the line: to a member of staff on their extension, or to another player at
 * the far telephone. Speaking on the telephone is just speaking.
 */
public final class DialogueListener {

    private DialogueListener() {}

    public static void register() {
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            String text = message.getContent().getString();
            if (text.isBlank() || text.startsWith("/")) {
                return;
            }
            if (routeDownTheLine(sender, text)) {
                return;
            }
            DialogueEngine.onChat(sender, text);
        });
    }

    /** If the speaker is on a call, send the words to the other end. */
    private static boolean routeDownTheLine(ServerPlayerEntity sender, String text) {
        MinecraftServer server = sender.getServer();
        if (server == null) {
            return false;
        }
        TelephonyState tel = TelephonyState.get(server);
        int number = tel.talkingLineFor(sender);
        if (number < 0) {
            return false;
        }
        KommutatorExchange.Line line = tel.exchange().line(number);
        if (line == null || line.peer < 0) {
            return true;
        }
        ServerWorld world = sender.getServerWorld();
        int peer = line.peer;

        if (tel.registry().isExtension(peer)) {
            CspPersonEntity worker = tel.workerAt(world, peer);
            if (worker == null) {
                sender.sendMessage(Text.literal("The line is silent — no one is at that extension.")
                    .formatted(Formatting.GRAY), true);
                return true;
            }
            sender.sendMessage(Text.literal("(you) " + text).formatted(Formatting.DARK_GRAY), true);
            WorkerSpeech.handle(worker, sender, DialogueIntent.fromText(text));
            return true;
        }

        ServerPlayerEntity other = tel.playerAtLine(world, peer);
        if (other != null) {
            other.sendMessage(Text.literal("Line " + number + ": " + text)
                .formatted(Formatting.YELLOW), false);
            sender.sendMessage(Text.literal("(down the line)").formatted(Formatting.DARK_GRAY), true);
        } else {
            sender.sendMessage(Text.literal("No one answers at the other end.")
                .formatted(Formatting.GRAY), true);
        }
        return true;
    }
}
