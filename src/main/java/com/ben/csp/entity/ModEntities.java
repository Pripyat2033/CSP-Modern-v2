package com.ben.csp.entity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Entity registration for CSP-Modern. Every field below is a live EntityType
 * registered with the game, and is referenced by the client renderers in
 * src/client/java.
 */
public class ModEntities {

    public static final String MOD_ID = "csp-modern";

    public static final EntityType<TolkachEntity> TOLKACH = register(
        "tolkach", SpawnGroup.CREATURE, 0.6f, 1.8f,
        FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, TolkachEntity::new));

    public static final EntityType<StroitelEntity> STROITEL = register(
        "stroitel", SpawnGroup.CREATURE, 0.6f, 1.8f,
        FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, StroitelEntity::new));

    public static final EntityType<ProrabEntity> PRORAB = register(
        "prorab", SpawnGroup.CREATURE, 0.6f, 1.8f,
        FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, ProrabEntity::new));

    public static final EntityType<BrigadierEntity> BRIGADIER = register(
        "brigadier", SpawnGroup.CREATURE, 0.6f, 1.8f,
        FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, BrigadierEntity::new));

    public static final EntityType<MasterEntity> MASTER = register(
        "master", SpawnGroup.CREATURE, 0.6f, 1.8f,
        FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, MasterEntity::new));

    public static final EntityType<GeodezistEntity> GEODEZIST = register(
        "geodezist", SpawnGroup.CREATURE, 0.6f, 1.8f,
        FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, GeodezistEntity::new));

    public static final EntityType<KommutatorOperatorEntity> KOMMUTATOR = register(
        "kommutator", SpawnGroup.CREATURE, 0.6f, 1.8f,
        FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, KommutatorOperatorEntity::new));

    public static final EntityType<UchetchikEntity> UCHETCHIK = register(
        "uchetchik", SpawnGroup.CREATURE, 0.6f, 1.8f,
        FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, UchetchikEntity::new));

    public static final EntityType<SekretarEntity> SEKRETAR = register(
        "sekretar", SpawnGroup.CREATURE, 0.6f, 1.8f,
        FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, SekretarEntity::new));

    public static final EntityType<NachalnikSnabzheniyaEntity> NACHALNIK_SNABZHENIYA = register(
        "nachalnik_snabzheniya", SpawnGroup.CREATURE, 0.6f, 1.8f,
        FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, NachalnikSnabzheniyaEntity::new));

    public static final EntityType<EnterpriseDirectorEntity> ENTERPRISE_DIRECTOR = register(
        "enterprise_director", SpawnGroup.CREATURE, 0.6f, 1.8f,
        FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, EnterpriseDirectorEntity::new));

    public static final EntityType<FacilityDirectorEntity> FACILITY_DIRECTOR = register(
        "facility_director", SpawnGroup.CREATURE, 0.6f, 1.8f,
        FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, FacilityDirectorEntity::new));

    public static final EntityType<GlavnyInzhenerEntity> GLAVNY_INZHENER = register(
        "glavny_inzhener", SpawnGroup.CREATURE, 0.6f, 1.8f,
        FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, GlavnyInzhenerEntity::new));

    public static final EntityType<BargeEntity> BARGE = register(
        "barge", SpawnGroup.MISC, 4.0f, 1.5f,
        FabricEntityTypeBuilder.create(SpawnGroup.MISC, BargeEntity::new));

    private static <T extends net.minecraft.entity.Entity> EntityType<T> register(
            String name, SpawnGroup group, float width, float height,
            FabricEntityTypeBuilder<T> builder) {
        // group is carried by the builder; kept in the signature for readability
        // of the call sites above.
        EntityType<T> type = builder.dimensions(EntityDimensions.fixed(width, height)).build();
        return Registry.register(Registries.ENTITY_TYPE, Identifier.of(MOD_ID, name), type);
    }

    public static void init() {
        // Static initialisers above perform registration on class load.
        // Referenced here so the class is definitely loaded during mod init.
        String registered = "tolkach, stroitel, prorab, brigadier, master, geodezist, uchetchik, kommutator, "
                + "sekretar, nachalnik_snabzheniya, enterprise_director, "
                + "facility_director, glavny_inzhener, barge";
        org.slf4j.LoggerFactory.getLogger("csp-modern").info("Registered entities: {}", registered);
    }

    public static String getModId() {
        return MOD_ID;
    }
}
