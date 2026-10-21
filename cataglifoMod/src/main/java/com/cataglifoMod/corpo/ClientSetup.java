package com.cataglifoMod.corpo;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// Só carrega no cliente (value = Dist.CLIENT), então um servidor dedicado não quebra.
@Mod.EventBusSubscriber(modid = CorpoMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void registrarRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(CorpoMod.CORPO.get(), CorpoRenderer::new);
    }
}
