package com.ben.csp.entity.ai;

import com.ben.csp.entity.CspPersonEntity;
import com.ben.csp.entity.WorkerRole;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates the lines a worker says when a player talks to them. The greeting
 * varies with the worker's {@link Dosye} trait and the body of the report is
 * the dossier itself, in the register a 1986 ChNPP construction worker would use.
 */
public final class WorkerChat {

    private WorkerChat() {}

    public static void speak(CspPersonEntity worker, PlayerEntity player) {
        Dosye d = worker.getDosye();
        WorkerRole role = worker.getRole();

        if (d == null) {
            player.sendMessage(Text.literal(role.getId() + ": \"...\"").formatted(Formatting.GRAY), false);
            return;
        }

        player.sendMessage(Text.literal(d.getFullName() + " (" + roleLabel(role) + ")")
            .formatted(Formatting.GOLD), false);
        player.sendMessage(Text.literal("\"" + greeting(d) + "\"").formatted(Formatting.WHITE), false);
        player.sendMessage(Text.literal(dossierLine(worker, d)).formatted(Formatting.GRAY), false);

        if (!d.getKompromat().isEmpty() && worker.getRandom().nextFloat() < 0.5f) {
            player.sendMessage(Text.literal("(lowering voice) \"" + d.getKompromat().get(0) + "\"")
                .formatted(Formatting.DARK_GRAY, Formatting.ITALIC), false);
        }
    }

    private static String roleLabel(WorkerRole role) {
        return switch (role) {
            case TOLKACH -> "labourer";
            case STROITEL -> "construction worker";
            case GEODEZIST -> "surveyor";
            case UCHETCHIK -> "record keeper";
            case BRIGADIER -> "brigade leader";
            case SEKRETAR -> "secretary";
            case NACHALNIK_SNABZHENIYA -> "supply chief";
            case MASTER -> "foreman";
            case PRORAB -> "site superintendent";
            case GLAVNY_INZHENER -> "chief engineer";
            case FACILITY_DIRECTOR -> "facility director";
            case ENTERPRISE_DIRECTOR -> "enterprise director";
            case KOMMUTATOR -> "switchboard operator";
        };
    }

    private static String greeting(Dosye d) {
        List<String> pool = switch (d.getTrait()) {
            case ANXIOUS -> List.of(
                "Is the form signed? Everything must be signed.",
                "I don't want trouble. The plan is behind again.");
            case OPTIMISTIC -> List.of(
                "We'll have this up before the deadline, comrade.",
                "Good day! Together the collective gets it done.");
            case PRAGMATIC -> List.of(
                "Show me the drawings and I'll tell you what's missing.",
                "Materials first. Talk is cheap.");
            case DUTIFUL -> List.of(
                "Orders are orders. What is the task?",
                "As directed, comrade.");
            case RECKLESS -> List.of(
                "It'll hold. Don't worry about the numbers.",
                "We can cut that corner, no one will notice.");
            case LOYAL -> List.of(
                "For the plant, and for the Party.",
                "Whatever the enterprise needs.");
        };
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    private static String dossierLine(CspPersonEntity worker, Dosye d) {
        String enterprise = worker.hasEnterprise() ? worker.getEnterpriseName() : "unassigned";
        return "Born " + d.getBirthplace() + ". " + d.getNationality()
            + ". Party: " + party(d.getPartyStatus())
            + ". Assessment: " + assessment(d.getKharakteristika())
            + ". Enterprise: " + enterprise + ".";
    }

    private static String party(Dosye.PartyStatus status) {
        return switch (status) {
            case CANDIDATE -> "candidate";
            case MEMBER -> "member";
            case NON_MEMBER -> "non-member";
            case EX_COMUNIST -> "expelled";
            case ELITE -> "nomenklatura";
        };
    }

    private static String assessment(Dosye.Kharakteristika k) {
        return switch (k) {
            case POSITIVE -> "positive";
            case NEUTRAL -> "neutral";
            case NEGATIVE -> "negative";
            case UNDER_INVESTIGATION -> "under investigation";
        };
    }
}
