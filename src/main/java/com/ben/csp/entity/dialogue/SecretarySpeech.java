package com.ben.csp.entity.dialogue;

import com.ben.csp.entity.CspPersonEntity;
import com.ben.csp.prombaza.PrombazaState;
import com.ben.csp.zone.ZoneOperations;
import com.ben.csp.zone.ZoneState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

/**
 * The secretary is the way into the zone. In 1970 nobody typed a command into a
 * computer to run a construction site — the Director spoke to the staff, and the
 * secretary was the one who carried it: she held the papers, made the calls, and
 * knew the state of the works. So the whole enterprise side — founding the site,
 * the workforce, the delivery desk — is reached by <em>talking to her</em>.
 *
 * <p>She is not a chatbot: her answers are the zone's own reports, taken from the
 * same operations the operator's commands use ({@link ZoneOperations}). What she
 * says is what is true of the world.
 */
public final class SecretarySpeech {

    private SecretarySpeech() {}

    /** A zone intent, answered by the secretary. Returns true if she handled it. */
    public static boolean handle(CspPersonEntity worker, PlayerEntity player, DialogueIntent intent) {
        if (!handles(intent)) {
            return false;
        }
        MinecraftServer server = worker.getServer();
        if (server == null || !(worker.getWorld() instanceof ServerWorld world)) {
            say(worker, player, "One moment, comrade — I have lost my papers.");
            return true;
        }
        BlockPos at = worker.getBlockPos();

        switch (intent) {
            case FOUND_SITE -> {
                ZoneOperations.Result r = ZoneOperations.foundSite(
                    server, world, at, 120, player.getName().getString());
                speak(worker, player, r);
                return true;
            }
            case SITE_REPORT -> {
                speak(worker, player, ZoneOperations.siteReport(server));
                return true;
            }
            case MUSTER -> {
                speak(worker, player, ZoneOperations.muster(server, world, at, -1));
                return true;
            }
            case PROMBAZA_REPORT -> {
                String id = currentPrombazaId(server);
                if (id == null) {
                    say(worker, player, "There is no prombaza on the books yet, comrade.");
                    return true;
                }
                speak(worker, player, ZoneOperations.prombazaReport(server, id));
                return true;
            }
            case DELIVER -> {
                String id = currentPrombazaId(server);
                if (id == null) {
                    say(worker, player, "There is no siding to work yet, comrade.");
                    return true;
                }
                int standing = PrombazaState.get(server).yard(id).standingCount();
                if (standing == 0) {
                    say(worker, player, "Nothing is standing on the siding, comrade.");
                    return true;
                }
                long day = world.getTimeOfDay() / 24000L + 1L;
                speak(worker, player, ZoneOperations.workWagons(server, id, standing, day));
                return true;
            }
            default -> {
                return false;   // not a zone matter; let the usual speech have it
            }
        }
    }

    /** The secretary's own words for a greeting or a question about herself. */
    public static void smallTalk(CspPersonEntity worker, PlayerEntity player, DialogueIntent intent) {
        switch (intent) {
            case GREET -> say(worker, player,
                "Good day, comrade Director. The papers are on the desk.");
            case WHO -> say(worker, player,
                "The secretary of the prombaza, comrade. Everything comes through this office.");
            case PLACE -> say(worker, player,
                "This is the production base. Speak to me and I will carry it: the site, the "
                + "men, the deliveries.");
            case HELP -> say(worker, player,
                "Speak to me plainly, comrade. Say \"found the site\", \"how is the site\", "
                + "\"muster the workforce\", \"the desk\", or \"work the wagons\".");
            default -> { }
        }
    }

    /** Whether an intent is one the secretary answers — a zone matter, not a personal order. */
    public static boolean handles(DialogueIntent intent) {
        return switch (intent) {
            case FOUND_SITE, SITE_REPORT, MUSTER, PROMBAZA_REPORT, DELIVER -> true;
            default -> false;
        };
    }

    /** How the secretary phrases the zone's report lines. */
    private static void speak(CspPersonEntity worker, PlayerEntity player, ZoneOperations.Result r) {
        if (r.lines().isEmpty()) {
            return;
        }
        for (String line : r.lines()) {
            say(worker, player, line);
        }
    }

    /** The prombaza the zone stands on, if it is standing. */
    private static String currentPrombazaId(MinecraftServer server) {
        ZoneState state = ZoneState.get(server);
        if (state.isSpawned()) {
            return state.zone().prombazaId();
        }
        var reg = PrombazaState.get(server).registry();
        if (!reg.isEmpty()) {
            return reg.all().get(reg.all().size() - 1).id();
        }
        return null;
    }

    private static void say(CspPersonEntity worker, PlayerEntity player, String line) {
        String name = worker.getDosye() != null ? worker.getDosye().getFullName()
            : "Secretary";
        player.sendMessage(Text.literal(name + ": \"" + line + "\"")
            .formatted(Formatting.WHITE), false);
    }
}
