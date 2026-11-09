package com.ben.csp.item;

import com.ben.csp.entity.ModEntities;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Spawn egg registration for every CSP-Modern entity. Each egg is a real
 * SpawnEggItem bound to its EntityType and added to the vanilla spawn-eggs
 * creative tab.
 */
public class ModItemsSpawnEggs {

    private static final String MOD_ID = "csp-modern";

    public static void registerModSpawnEggs() {
        reg("tolkach", ModEntities.TOLKACH, 0x00FF00, 0x003300);
        reg("stroitel", ModEntities.STROITEL, 0xFF8800, 0x8B4500);
        reg("prorab", ModEntities.PRORAB, 0xFFFF00, 0x8B8B00);
        reg("brigadier", ModEntities.BRIGADIER, 0xFF4444, 0x8B0000);
        reg("master", ModEntities.MASTER, 0xAA66FF, 0x4B0082);
        reg("geodezist", ModEntities.GEODEZIST, 0x44FF88, 0x006644);
        reg("uchetchik", ModEntities.UCHETCHIK, 0x8888FF, 0x00008B);
        reg("sekretar", ModEntities.SEKRETAR, 0xFFAACC, 0x8B3A62);
        reg("nachalnik_snabzheniya", ModEntities.NACHALNIK_SNABZHENIYA, 0x66DDDD, 0x006666);
        reg("enterprise_director", ModEntities.ENTERPRISE_DIRECTOR, 0xFFD700, 0x8B6914);
        reg("facility_director", ModEntities.FACILITY_DIRECTOR, 0xC0C0C0, 0x696969);
        reg("glavny_inzhener", ModEntities.GLAVNY_INZHENER, 0xFFFFFF, 0x404040);
        reg("barge", ModEntities.BARGE, 0x8B5A2B, 0x3B2A1A);
    }

    private static <T extends MobEntity> void reg(
            String name, EntityType<T> type, int primaryColor, int secondaryColor) {
        Identifier id = Identifier.of(MOD_ID, name + "_spawn_egg");
        Item egg = new SpawnEggItem(type, primaryColor, secondaryColor, new Item.Settings());
        Registry.register(Registries.ITEM, id, egg);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(entries -> entries.add(egg));
    }
}
