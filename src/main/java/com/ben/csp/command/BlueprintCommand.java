package com.ben.csp.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

/**
 * DECOMMISSIONED: This command was for manually creating blueprints from in-world selections.
 * This functionality has been superseded by the procedural GosstroyEngine and pre-defined JSON blueprints.
 * The code is kept for reference but is no longer active.
 */
public class BlueprintCommand {

    private static final SimpleCommandExceptionType DECOMMISSIONED_EXCEPTION = new SimpleCommandExceptionType(Text.literal("This command is decommissioned. Blueprints are now managed by the Gosstroy Engine."));

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        // Registration is intentionally left blank as this command is obsolete.
        dispatcher.register(CommandManager.literal("blueprint")
            .then(CommandManager.literal("save")
                .then(CommandManager.argument("name", StringArgumentType.word())
                    .then(CommandManager.argument("structure_type", StringArgumentType.word())
                        .executes(BlueprintCommand::runSave)
                    )
                )
            )
        );
    }

    private static int runSave(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        throw DECOMMISSIONED_EXCEPTION.create();
    }
}