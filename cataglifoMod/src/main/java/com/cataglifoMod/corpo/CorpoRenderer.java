package com.cataglifoMod.corpo;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

/**
 * Define COMO o mob aparece na tela. Por enquanto reaproveita o modelo
 * e a textura do zumbi, então não precisa criar nenhum asset para testar.
 */
public class CorpoRenderer extends HumanoidMobRenderer<CorpoEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {

    private static final Identifier TEXTURA =
            Identifier.fromNamespaceAndPath("cataglifo", "textures/entity/cataglyphis-bicolor.png");

    public CorpoRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
    }

    // Minecraft 26.3 separa a entidade do estado usado na renderização.
    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        return TEXTURA;
    }
}
