package com.ben.csp.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/**
 * The ultimate "Or Better" NPC. This entity is not controlled by the player.
 * He represents the will of the central government (Gosplan, Ministries) and acts
 * as the interface to the "external world" simulation.
 */
public class GosplanRepresentativeEntity extends PathAwareEntity {

    public GosplanRepresentativeEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createRepresentativeAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 100.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3);
    }

    @Override
    protected void initGoals() {
        // Standard goals are disabled. Its behavior will be dictated by a high-level
        // "ExternalWorldManager" that simulates directives from Moscow.
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new LookAtEntityGoal(this, PlayerEntity.class, 12.0f));
    }

    @Override
    public boolean isPushedByFluids() {
        return false;
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (!this.getWorld().isClient()) {
            // His dialogue would be driven by the state of the project.
            player.sendMessage(Text.literal("<Gosplan Representative> Director. Moscow is watching your progress closely. Do not disappoint."), false);
        }
        return ActionResult.SUCCESS;
    }
}