package com.cataglifoMod.corpo;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
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

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

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

    // Mantém a orientação própria e expira comandos mesmo sem conexão Python.
    private final ControleAcao controle = new ControleAcao();
    private boolean iniciado;

    // ---- API para o agente externo (ver ApiAgente) ----
    private Vec3 casa;                           // primeira posição vista; destino do /reset padrão
    private float casaYaw;
    private CompletableFuture<String> passoPendente; // resposta do /passo em andamento (só no servidor)

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

    /** Chamar somente na thread do servidor. O comando padrão dura cinco ticks. */
    public void definirAcao(float frente, float lado, float giraYaw, float giraPitch, boolean pular) {
        definirAcao(frente, lado, giraYaw, giraPitch, pular, ControleAcao.DURACAO_PADRAO);
    }

    public void definirAcao(float frente, float lado, float giraYaw, float giraPitch,
                            boolean pular, int duracaoTicks) {
        exigirServidor();
        if (!isAlive()) throw new IllegalStateException("Entidade morta não recebe ações");
        inicializarOrientacao(); // Antes da primeira ação, para não perder a rotação recebida.
        controle.definir(frente, lado, giraYaw, giraPitch, pular, duracaoTicks);
    }

    public void pararAcao() {
        exigirServidor();
        controle.parar();
        getNavigation().stop();
        getMoveControl().strafe(0, 0);
        setXxa(0);
        setZza(0);
        setJumping(false);
        setDeltaMovement(Vec3.ZERO);
    }

    /** Reset local para testes; episódios e recompensas de RL ainda exigem um gerenciador. */
    public void resetarCorpo(Vec3 inicio, float yaw) {
        exigirServidor();
        if (!Double.isFinite(inicio.x) || !Double.isFinite(inicio.y)
                || !Double.isFinite(inicio.z) || !Float.isFinite(yaw)) {
            throw new IllegalArgumentException("Reset exige posição e orientação finitas");
        }
        // A morte deve criar uma nova entidade; não ressuscitamos um objeto em remoção.
        if (!isAlive()) throw new IllegalStateException("Crie outra entidade após a morte");
        pararAcao();
        setPos(inicio.x, inicio.y, inicio.z);
        setHealth(getMaxHealth());
        clearFire();
        fallDistance = 0;
        controle.orientar(yaw, 0);
        iniciado = true;
        aplicarOrientacao();
    }

    private void exigirServidor() {
        if (!(level() instanceof ServerLevel servidor) || !servidor.getServer().isSameThread()) {
            throw new IllegalStateException("Acesse o corpo pela thread do servidor");
        }
    }

    private void inicializarOrientacao() {
        if (!iniciado) {
            controle.orientar(getYRot(), getXRot());
            iniciado = true;
        }
    }

    private void aplicarOrientacao() {
        setYRot(controle.yaw());
        setXRot(controle.pitch());
        setYBodyRot(controle.yaw());
        setYHeadRot(controle.yaw());
    }

    // A assinatura recebe ServerLevel no Minecraft 26.3.
    @Override
    protected void customServerAiStep(ServerLevel servidor) {
        super.customServerAiStep(servidor);
        inicializarOrientacao();
        aplicarOrientacao();
        getMoveControl().strafe(controle.frente(), controle.lado());
        if (controle.pular()) getJumpControl().jump();
        controle.avancar();
    }

    // ================= API (agente externo) =================

    /** Agenda {@code tarefa} na thread do servidor; pode ser chamado de qualquer thread. */
    <T> CompletableFuture<T> noServidor(Supplier<T> tarefa) {
        CompletableFuture<T> futuro = new CompletableFuture<>();
        if (!(level() instanceof ServerLevel servidor)) {
            futuro.completeExceptionally(new IllegalStateException("Corpo fora do servidor"));
            return futuro;
        }
        servidor.getServer().execute(() -> {
            try {
                futuro.complete(tarefa.get());
            } catch (Throwable erro) {
                futuro.completeExceptionally(erro);
            }
        });
        return futuro;
    }

    /**
     * Aplica a ação e devolve um futuro que completa com a observação (JSON) quando os ticks da
     * ação terminam ou o corpo morre. Um passo novo substitui o anterior. Só na thread do servidor.
     */
    CompletableFuture<String> iniciarPasso(float frente, float lado, float giraYaw, float giraPitch,
                                           boolean pular, int ticks) {
        exigirServidor();
        definirAcao(frente, lado, giraYaw, giraPitch, pular, ticks); // valida antes de mexer no passo
        cancelarPasso("Substituído por um passo mais novo");
        CompletableFuture<String> futuro = new CompletableFuture<>();
        passoPendente = futuro;
        return futuro;
    }

    void cancelarPasso(String motivo) {
        if (passoPendente != null) {
            passoPendente.completeExceptionally(new IllegalStateException(motivo));
            passoPendente = null;
        }
    }

    /** Reset pedido pela API: sem destino, volta ao ponto onde o corpo foi visto pela primeira vez. */
    void reiniciarApi(Vec3 destino, Float yaw) {
        exigirServidor();
        cancelarPasso("Passo interrompido por /reset");
        resetarCorpo(destino != null ? destino : casa, yaw != null ? yaw : casaYaw);
    }

    // ================= OLHOS =================

    public JsonObject montarObservacao() {
        exigirServidor();
        inicializarOrientacao();
        JsonObject obs = new JsonObject();
        obs.addProperty("schemaVersion", 2);
        obs.addProperty("id", getId());          // para escolher o corpo na API (?corpo=ID)
        obs.addProperty("tick", tickCount);      // ticks de vida da entidade; ajuda a descartar leituras velhas
        obs.addProperty("acaoAtiva", controle.ativa());
        obs.addProperty("morto", !isAlive());
        obs.addProperty("x", getX());
        obs.addProperty("y", getY());
        obs.addProperty("z", getZ());
        obs.addProperty("vida", getHealth());
        obs.addProperty("noChao", onGround());
        obs.addProperty("emLava", isInLava());
        obs.addProperty("yaw", controle.yaw());
        obs.addProperty("pitch", controle.pitch());

        JsonArray raios = new JsonArray();
        Vec3 olho = getEyePosition();
        for (float dPitch : DESVIOS_PITCH) {
            for (float dYaw : DESVIOS_YAW) {
                Vec3 dir = calculateViewVector(controle.pitch() + dPitch, controle.yaw() + dYaw);
                Vec3 fim = olho.add(dir.scale(ALCANCE));
                // Fluid.ANY faz o raio "enxergar" lava e água, não só blocos sólidos
                BlockHitResult hit = level().clip(new ClipContext(
                        olho, fim, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this));

                JsonObject r = new JsonObject();
                r.addProperty("dyaw", dYaw);
                r.addProperty("dpitch", dPitch);
                if (hit.getType() == HitResult.Type.MISS) {
                    // Sem colisão representa caminho livre, não uma distância negativa.
                    r.addProperty("acertou", false);
                    r.addProperty("dist", ALCANCE);
                    r.addProperty("distNormalizada", 1.0);
                    r.addProperty("bloco", "minecraft:air");
                    r.addProperty("fluido", "minecraft:empty");
                } else {
                    double distancia = olho.distanceTo(hit.getLocation());
                    r.addProperty("acertou", true);
                    r.addProperty("dist", distancia);
                    r.addProperty("distNormalizada", Math.clamp(distancia / ALCANCE, 0, 1));
                    // Fluido separado identifica também água em blocos alagados.
                    r.addProperty("fluido", BuiltInRegistries.FLUID.getKey(
                            level().getFluidState(hit.getBlockPos()).getType()).toString());
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
        if (level().isClientSide()) return;
        aplicarOrientacao(); // reaplica yaw/pitch depois do LookControl
        if (casa == null) {
            casa = position();
            casaYaw = getYRot();
        }
        ApiAgente.registrar(this); // sobe a API HTTP na primeira vez
        // Fecha o /passo quando a ação expira ou o corpo morre (a resposta já inclui "morto").
        if (passoPendente != null && (!controle.ativa() || !isAlive())) {
            CompletableFuture<String> futuro = passoPendente;
            passoPendente = null;
            futuro.complete(montarObservacao().toString());
        }
        // Diagnóstico opt-in em DEBUG: não gerar 15 raios por entidade só para log INFO.
        if (tickCount % 20 == 0 && CorpoMod.LOGGER.isDebugEnabled()) {
            CorpoMod.LOGGER.debug("[corpo] {}", montarObservacao());
        }
    }

    // TODO: sobrescrever die(DamageSource) para avisar a IA (fim de episódio)
    //       e fazer o respawn/reset no centro do cubo.
}
