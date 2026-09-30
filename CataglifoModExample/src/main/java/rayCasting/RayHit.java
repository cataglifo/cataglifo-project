package rayCasting;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class RayHit {

    private final boolean hit;
    private final Vec3 hitPosition; //bibliteca 3d do forge para representar a posição do hit
    private final double distance;
    private final BlockState blockState;

    public RayHit(
        boolean hit,
        Vec3 hitPosition,
        double distance,
        BlockState blockState
    ) {
        this.hit = hit;
        this.hitPosition = hitPosition;
        this.distance = distance;
        this.blockState = blockState;
    }

    public Block getBlock(){
        return blockState.getBlock();
    }
}