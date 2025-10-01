package com.ben.csp.entity;

import org.jetbrains.annotations.Nullable;

import com.ben.csp.entity.ai.PerformGeodeticSurveyGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockBox;
import net.minecraft.world.World;

/**
 * Science Grade: A specialist NPC dispatched to perform geodetic surveys,
 * laying out the groundwork for construction projects by placing markers.
 */
public class GeodezistEntity extends PathAwareEntity {
    @Nullable
    private BlockBox surveyArea;

    public GeodezistEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createGeodezistAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        // The primary AI goal for this entity.
        this.goalSelector.add(1, new PerformGeodeticSurveyGoal(this));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
    }

    @Nullable
    public BlockBox getSurveyArea() { return this.surveyArea; }
    public void setSurveyArea(@Nullable BlockBox surveyArea) { this.surveyArea = surveyArea; }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (surveyArea != null) {
            nbt.putIntArray("SurveyArea", new int[]{surveyArea.getMinX(), surveyArea.getMinY(), surveyArea.getMinZ(), surveyArea.getMaxX(), surveyArea.getMaxY(), surveyArea.getMaxZ()});
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("SurveyArea")) {
            int[] coords = nbt.getIntArray("SurveyArea");
            if (coords.length == 6) {
                this.surveyArea = new BlockBox(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
            }
        }
    }
}