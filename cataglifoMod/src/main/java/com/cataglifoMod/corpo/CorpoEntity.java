package com.cataglifoMod.corpo;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/*
 * Corpo da IA. Estende PathfinderMob e NÃO adiciona nenhum goal,
 * então o mob fica parado até a IA mandar uma ação.
 *
 * Tudo aqui roda no SERVIDOR (lado lógico do jogo).
 */
public class CorpoEntity extends PathfinderMob {

    // ---- Configuração dos "olhos": leque de raios ----
    private static final double ALCANCE = 16.0;                 // blocos
    private static final float[] DESVIOS_YAW   = {-60, -30, 0, 30, 60};
    private static final float[] DESVIOS_PITCH = {0, 30, 60};   // positivo = olhando para baixo

    // Yaw/pitch próprios da IA. Guardamos em variáveis separadas porque o
    // LookControl do Mob tende a mexer na rotação por conta própria.
    private float aiYaw;
    private float aiPitch;
    private boolean iniciado = false;

    // Ação pendente, definida pela IA e aplicada no próximo tick
    private float acaoFrente, acaoLado;
    private boolean acaoPular;

    public CorpoEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired(); // impede o mob de sumir (despawn) sozinho
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    // ================= MÃOS =================

    /** A IA chama isso para definir o que o corpo faz. frente/lado em [-1, 1]. */
    public void definirAcao(float frente, float lado, float giraYaw, float giraPitch, boolean pular) {
        this.acaoFrente = Mth.clamp(frente, -1F, 1F);
        this.acaoLado = Mth.clamp(lado, -1F, 1F);
        this.aiYaw = Mth.wrapDegrees(this.aiYaw + giraYaw);
        this.aiPitch = Mth.clamp(this.aiPitch + giraPitch, -90F, 90F);
        this.acaoPular = pular;
    }

    // Chamado a cada tick no servidor, ANTES de o MoveControl/JumpControl rodarem
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!iniciado) {
            aiYaw = getYRot();
            iniciado = true;
        }
        setYRot(aiYaw);
        setYBodyRot(aiYaw);
        setYHeadRot(aiYaw);
        getMoveControl().strafe(acaoFrente, acaoLado); // precisa ser chamado todo tick
        if (acaoPular) {
            getJumpControl().jump();
            acaoPular = false;
        }
    }

    // ================= OLHOS =================

    public JsonObject montarObservacao() {
        JsonObject obs = new JsonObject();
        obs.addProperty("x", getX());
        obs.addProperty("y", getY());
        obs.addProperty("z", getZ());
        obs.addProperty("vida", getHealth());
        //obs.addProperty("noChao", onGround());
        //obs.addProperty("emLava", isInLava());
        obs.addProperty("yaw", aiYaw);
        obs.addProperty("pitch", aiPitch);

        JsonArray raios = new JsonArray();
        Vec3 olho = getEyePosition();
        for (float dPitch : DESVIOS_PITCH) {
            for (float dYaw : DESVIOS_YAW) {
                Vec3 dir = calculateViewVector(aiPitch + dPitch, aiYaw + dYaw);
                Vec3 fim = olho.add(dir.scale(ALCANCE));
                // Fluid.ANY faz o raio "enxergar" lava e água, não só blocos sólidos
                BlockHitResult hit = level().clip(new ClipContext(
                        olho, fim, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this));

                JsonObject r = new JsonObject();
                r.addProperty("dyaw", dYaw);
                r.addProperty("dpitch", dPitch);
                if (hit.getType() == HitResult.Type.MISS) {
                    r.addProperty("dist", -1.0);
                    r.addProperty("bloco", "ar");
                } else {
                    r.addProperty("dist", olho.distanceTo(hit.getLocation()));
                    BlockState estado = level().getBlockState(hit.getBlockPos());
                    r.addProperty("bloco", BuiltInRegistries.BLOCK.getKey(estado.getBlock()).toString());
                }
                raios.add(r);
            }
        }
        obs.add("raios", raios);
        return obs;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        // TESTE INICIAL: imprime a observação 1x por segundo no log.
        // Depois, trocar por envio via socket para o Python.
        if (tickCount % 20 == 0) {
            CorpoMod.LOGGER.info("[corpo] {}", montarObservacao());
        }
    }

    // TODO: sobrescrever die(DamageSource) para avisar a IA (fim de episódio)
    //       e fazer o respawn/reset no centro do cubo.
}
