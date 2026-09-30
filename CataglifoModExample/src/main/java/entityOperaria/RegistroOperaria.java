package entityOperaria;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public static final RegistroOperaria<EntityType<Operaria>> OPERARIA = 
    ENTITY_TYPES.register(
    "operaria", () -> EntityType.Builder.of(
        Operaria::new, MobCategory.MISC).sized(0.6F, 1.8F).build("operaria"));

