package com.ben.csp.entity.dialogue;

import com.ben.csp.telephony.KommutatorExchange;
import com.ben.csp.telephony.TelephonyState;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Turns plain chat into speech. When the Director types a sentence, the workers
 * within earshot answer and act. If instead the Director is holding a call on a
 * vertushka, the words go down the line to whoever is at the other end — speaking on
 * the telephone is just speaking.
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
        ServerPlayerEntity peer = line == null ? null
            : tel.playerAtLine(server.getOverworld(), line.peer);
        if (peer != null) {
            peer.sendMessage(Text.literal("Line " + number + ": " + text)
                .formatted(Formatting.YELLOW), false);
            sender.sendMessage(Text.literal("(down the line)").formatted(Formatting.DARK_GRAY), true);
        } else {
            sender.sendMessage(Text.literal("No one answers at the other end.")
                .formatted(Formatting.GRAY), true);
        }
        return true;
    }
}
