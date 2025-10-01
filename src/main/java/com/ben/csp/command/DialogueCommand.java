package com.ben.csp.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class DialogueCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("dialogue")
            .then(CommandManager.literal("start")
                .then(CommandManager.argument("npc", EntityArgumentType.entity())
                    .executes(DialogueCommand::startDialogue)
                )
            )
            .then(CommandManager.literal("choose")
                .then(CommandManager.argument("npc", EntityArgumentType.entity())
                    .then(CommandManager.argument("choice", IntegerArgumentType.integer(0))
                        .executes(DialogueCommand::chooseDialogue)
                    )
                )
            )
        );
    }

    private static int startDialogue(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        PathAwareEntity npc = (PathAwareEntity) EntityArgumentType.getEntity(context, "npc");

        List<DialogueTopic> topics = DialogueManager.getInstance().getAvailableTopics(npc);

        if (topics.isEmpty()) {
            player.sendMessage(Text.literal("<" + npc.getName().getString() + "> ..."), false);
            return 0;
        }

        player.sendMessage(Text.literal("Choose a topic:").formatted(Formatting.YELLOW), false);
        for (int i = 0; i < topics.size(); i++) {
            DialogueTopic topic = topics.get(i);
            MutableText choiceText = Text.literal("[" + (i + 1) + "] " + topic.getChoiceText().getString());
            String command = String.format("/dialogue choose @e[type=%s,distance=..10,limit=1] %d", npc.getType().getUntranslatedName(), i);
            choiceText.setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to choose this option")))
                .withColor(Formatting.AQUA));
            player.sendMessage(choiceText, false);
        }
        return 1;
    }

    private static int chooseDialogue(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        PathAwareEntity npc = (PathAwareEntity) EntityArgumentType.getEntity(context, "npc");
        int choice = IntegerArgumentType.getInteger(context, "choice");
        List<DialogueTopic> topics = DialogueManager.getInstance().getAvailableTopics(npc);
        if (choice >= 0 && choice < topics.size()) {
            topics.get(choice).execute(player, npc);
        }
        return 1;
    }
}