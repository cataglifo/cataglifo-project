package entityOperaria;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;


/*forge possui uma biblioteca para entidades que
ja possui as funções necessárias, getX, getY, getZ
ainda não sei o que seria nescessaario adicionar nessa classe para
nossas necessidades*/

//no fim o claudinho vai saber

public class Operaria extends PathfinderMob {
    public Operaria(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }
}