package com.cataglifoMod.corpo;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

/**
 * Liga os corpos do jogo à API HTTP. É a única parte da API que conhece Minecraft.
 *
 * O servidor HTTP sobe sozinho quando o primeiro corpo faz tick no servidor
 * (use /summon cataglifo:corpo). Fica em http://127.0.0.1:8765, e as threads são
 * daemon, então não impedem o jogo de fechar. Porta: -Dcataglifo.api.porta=NNNN.
 * Desligar: -Dcataglifo.api.desativada=true.
 */
final class ApiAgente implements PonteCorpo {
    static final int PORTA_PADRAO = 8765;
    /** Quanto a thread HTTP espera o jogo responder (um passo dura no máximo 20 ticks = 1 s). */
    private static final long LIMITE_MS = 10_000;

    private static final ApiAgente INSTANCIA = new ApiAgente();
    private static volatile boolean tentouIniciar;
    private static ApiServidor servidor;

    private final Set<CorpoEntity> corpos = ConcurrentHashMap.newKeySet();

    private ApiAgente() { }

    /** Chamado a cada tick do servidor por cada corpo; barato depois da primeira vez. */
    static void registrar(CorpoEntity corpo) {
        INSTANCIA.corpos.add(corpo);
        if (!tentouIniciar) iniciar();
    }

    private static synchronized void iniciar() {
        if (tentouIniciar) return;
        tentouIniciar = true; // uma tentativa só: se a porta estiver ocupada, não repete a cada tick
        if (Boolean.getBoolean("cataglifo.api.desativada")) {
            CorpoMod.LOGGER.info("[api] desativada por -Dcataglifo.api.desativada=true");
            return;
        }
        int porta = Integer.getInteger("cataglifo.api.porta", PORTA_PADRAO);
        try {
            servidor = new ApiServidor(INSTANCIA, porta, LIMITE_MS);
            CorpoMod.LOGGER.info("[api] ouvindo em http://127.0.0.1:{}", servidor.porta());
        } catch (IOException e) {
            CorpoMod.LOGGER.warn("[api] não foi possível abrir a porta {}: {}", porta, e.getMessage());
        }
    }

    // ------------------------------------------------------------ busca

    private CorpoEntity escolher(Integer id) {
        CorpoEntity primeiro = null;
        for (CorpoEntity c : corpos) {
            if (c.isRemoved()) {
                corpos.remove(c);
                continue;
            }
            if (id != null && c.getId() == id) return c;
            if (primeiro == null || (!primeiro.isAlive() && c.isAlive())) primeiro = c;
        }
        if (id != null) throw new ErroApi(404, "Nenhum corpo com id " + id);
        if (primeiro == null) throw new ErroApi(404, "Nenhum corpo no jogo. Use /summon cataglifo:corpo ~ ~ ~");
        return primeiro;
    }

    // ------------------------------------------------------------ rotas

    @Override
    public String saude() {
        JsonObject o = new JsonObject();
        o.addProperty("ok", true);
        o.addProperty("api", 1);
        JsonArray lista = new JsonArray();
        for (CorpoEntity c : corpos) {
            if (c.isRemoved()) continue;
            JsonObject item = new JsonObject();
            item.addProperty("id", c.getId());
            item.addProperty("vivo", c.isAlive());
            lista.add(item);
        }
        o.add("corpos", lista);
        return o.toString();
    }

    @Override
    public CompletableFuture<String> observar(Integer id) {
        CorpoEntity c = escolher(id);
        return c.noServidor(() -> c.montarObservacao().toString());
    }

    @Override
    public CompletableFuture<String> passo(Integer id, String jsonAcao) {
        CorpoEntity c = escolher(id);
        JsonObject o = objeto(jsonAcao, true);
        float frente = numero(o, "frente", 0);
        float lado = numero(o, "lado", 0);
        float yaw = numero(o, "yaw", 0);
        float pitch = numero(o, "pitch", 0);
        boolean pular = booleano(o, "pular", false);
        int ticks = inteiro(o, "ticks", ControleAcao.DURACAO_PADRAO);
        // noServidor devolve um futuro de futuro: o interno só completa quando os ticks terminam.
        return c.noServidor(() -> c.iniciarPasso(frente, lado, yaw, pitch, pular, ticks))
                .thenCompose(fim -> fim);
    }

    @Override
    public CompletableFuture<String> parar(Integer id) {
        CorpoEntity c = escolher(id);
        return c.noServidor(() -> {
            c.cancelarPasso("Passo interrompido por /parar");
            c.pararAcao();
            return "{\"ok\":true}";
        });
    }

    @Override
    public CompletableFuture<String> reiniciar(Integer id, String jsonReset) {
        CorpoEntity c = escolher(id);
        JsonObject o = objeto(jsonReset, false);
        boolean temPosicao = o.has("x") || o.has("y") || o.has("z");
        if (temPosicao && !(o.has("x") && o.has("y") && o.has("z"))) {
            throw new IllegalArgumentException("Informe x, y e z juntos, ou nenhum deles");
        }
        Vec3 destino = temPosicao ? new Vec3(numero(o, "x", 0), numero(o, "y", 0), numero(o, "z", 0)) : null;
        Float yaw = o.has("yaw") ? numero(o, "yaw", 0) : null;
        return c.noServidor(() -> {
            c.reiniciarApi(destino, yaw);
            return c.montarObservacao().toString();
        });
    }

    // ---------------------------------------------------------- parsing

    private static JsonObject objeto(String json, boolean obrigatorio) {
        if (json == null || json.isBlank()) {
            if (obrigatorio) throw new IllegalArgumentException("Corpo JSON obrigatório");
            return new JsonObject();
        }
        try {
            JsonElement e = JsonParser.parseString(json);
            if (!e.isJsonObject()) throw new IllegalArgumentException("O corpo deve ser um objeto JSON");
            return e.getAsJsonObject();
        } catch (JsonParseException e) {
            throw new IllegalArgumentException("JSON inválido: " + e.getMessage());
        }
    }

    private static float numero(JsonObject o, String chave, float padrao) {
        if (!o.has(chave) || o.get(chave).isJsonNull()) return padrao;
        JsonElement e = o.get(chave);
        if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("'" + chave + "' deve ser um número");
        }
        return e.getAsFloat();
    }

    private static int inteiro(JsonObject o, String chave, int padrao) {
        float v = numero(o, chave, padrao);
        if (v != Math.rint(v)) throw new IllegalArgumentException("'" + chave + "' deve ser inteiro");
        return (int) v;
    }

    private static boolean booleano(JsonObject o, String chave, boolean padrao) {
        if (!o.has(chave) || o.get(chave).isJsonNull()) return padrao;
        JsonElement e = o.get(chave);
        if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isBoolean()) {
            throw new IllegalArgumentException("'" + chave + "' deve ser true ou false");
        }
        return e.getAsBoolean();
    }
}
