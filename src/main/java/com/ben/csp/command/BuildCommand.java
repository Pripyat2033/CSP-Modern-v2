package com.ben.csp.command;

import com.ben.csp.entity.GeodezistEntity;
import com.ben.csp.entity.ModEntities;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;

public class BuildCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("csp")
            .then(CommandManager.literal("build")
                .then(CommandManager.literal("survey")
                    .then(CommandManager.argument("from", BlockPosArgumentType.blockPos())
                        .then(CommandManager.argument("to", BlockPosArgumentType.blockPos())
                            .executes(BuildCommand::executeSurvey)
                        )
                    )
                )
            )
        );
    }

    private static int executeSurvey(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        BlockPos from = BlockPosArgumentType.getBlockPos(context, "from");
        BlockPos to = BlockPosArgumentType.getBlockPos(context, "to");
        BlockBox surveyArea = BlockBox.create(from, to);
        GeodezistEntity geodezist = new GeodezistEntity(ModEntities.GEODEZIST, world);
        geodezist.setPos(source.getPosition().x, source.getPosition().y, source.getPosition().z);
        geodezist.setSurveyArea(surveyArea);
        world.spawnEntity(geodezist);
        source.sendFeedback(() -> Text.literal("Dispatched Geodezist to survey area " + surveyArea), false);
        return 1;
    }
}