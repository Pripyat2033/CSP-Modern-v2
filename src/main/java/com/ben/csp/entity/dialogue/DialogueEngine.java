package com.ben.csp.entity.dialogue;

import com.ben.csp.entity.CspPersonEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Director's mouthpiece. A spoken sentence is parsed against the world — who is
 * within earshot, who was last spoken to — and answered by rules. This is SHRDLU's
 * design, not a language model: the world is small and closed, so the answer is
 * derived, never invented, and it says so plainly when it does not understand.
 */
public final class DialogueEngine {

    private DialogueEngine() {}

    private static final double EARSHOT = 12.0;

    /** Context memory: the worker each player last spoke to (SHRDLU's "the cone"). */
    private static final Map<UUID, Integer> LAST_SPOKEN = new ConcurrentHashMap<>();

    public static void onChat(ServerPlayerEntity player, String text) {
        DialogueParse parse = DialogueParse.of(text);
        List<CspPersonEntity> near = nearby(player);
        if (near.isEmpty()) {
            player.sendMessage(Text.literal("There is no one within earshot, comrade.")
                .formatted(Formatting.GRAY), false);
            return;
        }

        List<CspPersonEntity> targets = resolveTargets(player, parse, near);
        DialogueIntent intent = parse.intent();

        if (targets.size() > 1) {
            for (CspPersonEntity worker : targets) {
                WorkerSpeech.act(worker, player, intent);
            }
            LAST_SPOKEN.put(player.getUuid(), targets.get(0).getId());
            player.sendMessage(Text.literal("(" + targets.size() + " workers: "
                + WorkerSpeech.groupReply(intent) + ")").formatted(Formatting.GRAY), false);
            return;
        }

        CspPersonEntity worker = targets.get(0);
        LAST_SPOKEN.put(player.getUuid(), worker.getId());
        WorkerSpeech.handle(worker, player, intent);
    }

    private static List<CspPersonEntity> resolveTargets(ServerPlayerEntity player,
            DialogueParse parse, List<CspPersonEntity> near) {
        switch (parse.target()) {
            case ALL_NEAR -> {
                return near;
            }
            case LAST -> {
                Integer id = LAST_SPOKEN.get(player.getUuid());
                if (id != null) {
                    for (CspPersonEntity w : near) {
                        if (w.getId() == id) {
                            return List.of(w);
                        }
                    }
                }
                return List.of(nearest(player, near));
            }
            default -> {
                return List.of(nearest(player, near));
            }
        }
    }

    private static CspPersonEntity nearest(ServerPlayerEntity player, List<CspPersonEntity> near) {
        return near.stream().min(Comparator.comparingDouble(player::squaredDistanceTo)).orElseThrow();
    }

    private static List<CspPersonEntity> nearby(ServerPlayerEntity player) {
        Box around = player.getBoundingBox().expand(EARSHOT);
        List<CspPersonEntity> found = new ArrayList<>(player.getServerWorld()
            .getEntitiesByClass(CspPersonEntity.class, around, CspPersonEntity::isAlive));
        found.sort(Comparator.comparingDouble(player::squaredDistanceTo));
        return found;
    }
}
