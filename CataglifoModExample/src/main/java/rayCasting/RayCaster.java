package rayCasting;

import java.io.Serializable;// permite transformar o objeto em bytes para salvar em arquivo

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class RayCaster implements Serializable {

    public static RayHit cast(Level level,  Vec3 origin, Vec3 direction, double maxDistance) {
        return new RayHit(null, null, 0, null); // Placeholder
    }
}