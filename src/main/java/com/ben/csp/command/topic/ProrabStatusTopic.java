package com.ben.csp.command.topic;

import com.ben.csp.command.DialogueTopic;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

public class ProrabStatusTopic implements DialogueTopic {
    @Override
    public String getId() {
        return "prorab_status";
    }

    @Override
    public Text getChoiceText() {
        return Text.literal("Ask about current status.");
    }

    @Override
    public boolean isAvailable(PathAwareEntity npc) {
        // This topic is always available for a Prorab.
        return true;
    }

    @Override
    public void execute(PlayerEntity player, PathAwareEntity npc) {
        player.sendMessage(Text.literal("<" + npc.getName().getString() + "> Status is nominal. All work proceeds according to plan."), false);
    }
}