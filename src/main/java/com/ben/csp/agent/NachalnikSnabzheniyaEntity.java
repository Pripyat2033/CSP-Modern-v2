package com.ben.csp.agent;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.Mob;

@Environment(value = EnvType.SERVER)
public class NachalnikSnabzheniyaEntity extends Mob {
    public static final EntityType<NachalnikSnabzheniyaEntity> TYPE = EntityType.Builder.<NachalnikSnabzheniyaEntity>create(NachalnikSnabzheniyaEntity::new, MobCategory.MOB)
        .sized(0.6f, 2.0f)
        .setShouldDropItems(true)
        .build("nachalnik_snabzheniya_entity");

    public NachalnikSnabzheniyaEntity(EntityType<NachalnikSnabzheniyaEntity> type, Level world) {
        super(type, world);
    }
}
