package com.ben.csp.client.render.entity;
import com.ben.csp.entity.*;
import com.ben.csp.CSPMod;
import com.ben.csp.entity.GlavnyInzhenerEntity;
import com.ben.csp.entity.ModModelLayers;
import com.ben.csp.client.render.model.entity.GlavnyInzhenerEntityModel;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class GlavnyInzhenerEntityRenderer extends MobEntityRenderer<GlavnyInzhenerEntity, GlavnyInzhenerEntityModel> {
    private static final Identifier TEXTURE = new Identifier(CSPMod.MOD_ID, "textures/entity/glavny_inzhener.png");

    public GlavnyInzhenerEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new GlavnyInzhenerEntityModel(context.getPart(ModModelLayers.GLAVNY_INZHENER)), 0.5f);
    }

    @Override
    public Identifier getTexture(GlavnyInzhenerEntity entity) {
        return TEXTURE;
    }
}