package com.ben.csp.client.render.model;

import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

/**
 * Model layer keys shared by entity and block-entity renderers.
 */
public class ModModelLayers {
    private static EntityModelLayer layer(String path) {
        return new EntityModelLayer(new Identifier("csp-modern", path), "main");
    }

    // Entity layers
    public static final EntityModelLayer TOLKACH = layer("tolkach");
    public static final EntityModelLayer BARGE = layer("barge");
    public static final EntityModelLayer BRIGADIER = layer("brigadier");
    public static final EntityModelLayer MASTER = layer("master");
    public static final EntityModelLayer PRORAB = layer("prorab");
    public static final EntityModelLayer STROITEL = layer("stroitel");
    public static final EntityModelLayer NACHALNIK_SNABZHENIYA = layer("nachalnik_snabzheniya");
    public static final EntityModelLayer UCHETCHIK = layer("uchetchik");
    public static final EntityModelLayer ENTERPRISE_DIRECTOR = layer("enterprise_director");
    public static final EntityModelLayer FACILITY_DIRECTOR = layer("facility_director");
    public static final EntityModelLayer SEKRETAR = layer("sekretar");
    public static final EntityModelLayer GEODEZIST = layer("geodezist");
    public static final EntityModelLayer GLAVNY_INZHENER = layer("glavny_inzhener");

    // Block entity layers
    public static final EntityModelLayer VERTUSHKA = layer("vertushka");
    public static final EntityModelLayer ARCHIVE_FILING_CABINET = layer("archive_filing_cabinet");
    public static final EntityModelLayer SKALA_TAPE_DRIVE = layer("skala_tape_drive");
}
