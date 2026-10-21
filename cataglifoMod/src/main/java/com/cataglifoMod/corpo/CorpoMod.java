package com.cataglifoMod.corpo; // troque pelo pacote do projeto de vocês

import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

// O valor de MODID precisa ser IGUAL ao modId do arquivo META-INF/mods.toml
@Mod(CorpoMod.MODID)
public class CorpoMod {
    public static final String MODID = "corpoia";
    public static final Logger LOGGER = LogUtils.getLogger();

    // 1) REGISTRO: é aqui que o Forge "fica sabendo" que o mob existe.
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);

    public static final RegistryObject<EntityType<CorpoEntity>> CORPO =
            ENTITIES.register("corpo", () -> EntityType.Builder
                    .<CorpoEntity>of(CorpoEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F) // largura x altura (tamanho de um jogador)
                    .build("corpo"));

    public CorpoMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITIES.register(modBus);
        modBus.addListener(this::registrarAtributos);
    }

    // 2) ATRIBUTOS: sem isso o jogo costuma crashar ao spawnar o mob.
    private void registrarAtributos(EntityAttributeCreationEvent event) {
        event.put(CORPO.get(), CorpoEntity.createAttributes().build());
    }
}
