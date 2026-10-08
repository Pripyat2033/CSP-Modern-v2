package com.ben.csp.client.render.entity;
import com.ben.csp.entity.*;
import com.ben.csp.CSPMod;
import com.ben.csp.entity.ModModelLayers;
import com.ben.csp.client.render.model.entity.GeodezistEntityModel;
import com.ben.csp.entity.GeodezistEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class GeodezistEntityRenderer extends MobEntityRenderer<GeodezistEntity, GeodezistEntityModel> {
    private static final Identifier TEXTURE = new Identifier(CSPMod.MOD_ID, "textures/entity/geodezist.png");

    public GeodezistEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new GeodezistEntityModel(context.getPart(ModModelLayers.GEODEZIST)), 0.5f);
    }

    @Override
    public Identifier getTexture(GeodezistEntity entity) {
        return TEXTURE;
    }
}