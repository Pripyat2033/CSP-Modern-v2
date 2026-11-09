package com.ben.csp.entity.dialogue;

import com.ben.csp.entity.CspPersonEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

/**
 * What a worker says, and does, when the Director speaks to it. Replies are short
 * and in the register of the period; orders change the worker's behaviour.
 */
public final class WorkerSpeech {

    private WorkerSpeech() {}

    public static void handle(CspPersonEntity worker, PlayerEntity player, DialogueIntent intent) {
        switch (intent) {
            case GREET -> say(worker, player, "Comrade Director.");
            case COME -> {
                worker.orderCome(player);
                say(worker, player, "At once.");
            }
            case FOLLOW -> {
                worker.orderFollow(player);
                say(worker, player, "Right behind you, comrade.");
            }
            case STAND_DOWN -> {
                worker.orderStandDown();
                say(worker, player, "As you say. Standing easy.");
            }
            case GO_POST -> {
                BlockPos post = worker.getWorkplacePos();
                if (post == null) {
                    say(worker, player, "I have no post assigned yet, comrade.");
                } else {
                    worker.orderGoToPost();
                    say(worker, player, "Back to the post, then.");
                }
            }
            case STATUS -> say(worker, player, status(worker));
            case WHO -> say(worker, player, who(worker));
            case PLACE -> say(worker, player, place(worker));
            case HELP -> say(worker, player,
                "You can speak to me plainly: \"come here\", \"follow me\", \"stand down\", "
                + "\"to your post\", \"report\", \"who are you\".");
            case UNKNOWN -> say(worker, player,
                "I don't follow, comrade. Try \"come here\", \"follow me\", \"stand down\", "
                + "\"to your post\" or \"report\".");
        }
    }

    private static String status(CspPersonEntity worker) {
        StringBuilder sb = new StringBuilder();
        if (worker.getWorkplacePos() != null) {
            BlockPos p = worker.getWorkplacePos();
            sb.append("At my post (").append(p.getX()).append(", ").append(p.getZ()).append("). ");
        } else {
            sb.append("No post assigned. ");
        }
        if (worker.hasEnterprise()) {
            sb.append("Enterprise: ").append(worker.getEnterpriseName()).append(".");
        } else {
            sb.append("Not yet assigned to an enterprise.");
        }
        return sb.toString();
    }

    private static String who(CspPersonEntity worker) {
        String role = CspPersonEntity.roleTag(worker.getRole());
        if (worker.getDosye() != null) {
            return worker.getDosye().getFullName() + " " + role + ".";
        }
        return "A " + role + ", comrade.";
    }

    private static String place(CspPersonEntity worker) {
        if (worker.hasEnterprise()) {
            return "This is the " + worker.getEnterpriseName() + " site.";
        }
        return "This is the prombaza — the production base, where the trusts stand up "
            + "their workshops before going on to the station.";
    }

    private static void say(CspPersonEntity worker, PlayerEntity player, String line) {
        String name = worker.getDosye() != null ? worker.getDosye().getFullName()
            : CspPersonEntity.roleTag(worker.getRole());
        player.sendMessage(Text.literal(name + ": \"" + line + "\"")
            .formatted(Formatting.WHITE), false);
    }
}
