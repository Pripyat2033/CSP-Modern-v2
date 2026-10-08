package com.ben.csp.agent;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.brain.Bean;
import net.minecraft.world.entity.ai.brain.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

@Environment(value = EnvType.SERVER)
public class GlavnyInzhenerEntity extends Mob {
    public static final EntityType<GlavnyInzhenerEntity> TYPE = EntityType.Builder.<GlavnyInzhenerEntity>create(GlavnyInzhenerEntity::new, MobCategory.MOB)
        .sized(0.6f, 2.0f)
        .setShouldDropItems(true)
        .build("glavny_inzhener_entity");

    public GlavnyInzhenerEntity(EntityType<GlavnyInzhenerEntity> type, Level world) {
        super(type, world);
    }

    @Override
    protected void brainInitialization(HolderLookup.Provider registries) {
    }
}
