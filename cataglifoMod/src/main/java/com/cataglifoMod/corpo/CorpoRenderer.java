package com.cataglifoMod.corpo;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Define COMO o mob aparece na tela. Por enquanto reaproveita o modelo
 * e a textura do zumbi, então não precisa criar nenhum asset para testar.
 */
public class CorpoRenderer extends HumanoidMobRenderer<CorpoEntity, HumanoidModel<CorpoEntity>> {

    private static final ResourceLocation TEXTURA =
            ResourceLocation.withDefaultNamespace("textures/entity/zombie/zombie.png");

    public CorpoRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(CorpoEntity entity) {
        return TEXTURA;
    }
}
