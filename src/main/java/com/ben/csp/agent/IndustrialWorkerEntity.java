package com.ben.csp.agent;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.Mob;

@Environment(value = EnvType.SERVER)
public class IndustrialWorkerEntity extends Mob {
    public static final EntityType<IndustrialWorkerEntity> TYPE = EntityType.Builder.<IndustrialWorkerEntity>create(IndustrialWorkerEntity::new, MobCategory.MOB)
        .sized(0.6f, 1.8f)
        .setShouldDropItems(true)
        .build("industrial_worker_entity");

    public IndustrialWorkerEntity(EntityType<IndustrialWorkerEntity> type, Level world) {
        super(type, world);
    }
}
