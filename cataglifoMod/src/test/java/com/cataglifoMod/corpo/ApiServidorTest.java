package com.cataglifoMod.corpo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

/**
 * Testa a camada HTTP sem Minecraft: uma ponte falsa responde no lugar do jogo.
 * Não é JUnit; roda por main() (veja a tarefa apiServidorTest no build.gradle).
 */
public final class ApiServidorTest {
    private static int falhas;
    private static Integer ultimoCorpo;
    private static String ultimoCorpoJson;

    public static void main(String[] args) throws Exception {
        PonteCorpo falsa = new PonteCorpo() {
            public String saude() { return "{\"ok\":true,\"api\":1,\"corpos\":[]}"; }
            public CompletableFuture<String> observar(Integer c) {
                ultimoCorpo = c;
                return CompletableFuture.completedFuture("{\"schemaVersion\":2,\"id\":1}");
            }
            public CompletableFuture<String> passo(Integer c, String json) {
                ultimoCorpo = c;
                ultimoCorpoJson = json;
                if (json.contains("invalido")) throw new IllegalArgumentException("ação inválida");
                if (json.contains("conflito")) {
                    return CompletableFuture.failedFuture(new IllegalStateException("Entidade morta"));
                }
                if (json.contains("trava")) return new CompletableFuture<>(); // nunca completa
                if (json.contains("naoexiste")) throw new ErroApi(404, "Nenhum corpo");
                return CompletableFuture.completedFuture("{\"passo\":true}");
            }
            public CompletableFuture<String> parar(Integer c) {
                return CompletableFuture.completedFuture("{\"ok\":true}");
            }
            public CompletableFuture<String> reiniciar(Integer c, String json) {
                ultimoCorpoJson = json;
                return CompletableFuture.completedFuture("{\"reset\":true}");
            }
        };

        try (ApiServidor api = new ApiServidor(falsa, 0, 400)) {
            String base = "http://127.0.0.1:" + api.porta();
            HttpClient http = HttpClient.newHttpClient();

            Resp r = get(http, base + "/saude");
            confere("GET /saude", r, 200, "\"api\":1");

            r = get(http, base + "/observacao");
            confere("GET /observacao", r, 200, "\"schemaVersion\":2");
            verdade("sem ?corpo usa null", ultimoCorpo == null);

            get(http, base + "/observacao?corpo=7");
            verdade("?corpo=7 chega à ponte", ultimoCorpo != null && ultimoCorpo == 7);

            r = get(http, base + "/observacao?corpo=abc");
            confere("corpo não numérico", r, 400, "inteiro");

            r = post(http, base + "/passo", "{\"frente\":1,\"ticks\":5}");
            confere("POST /passo", r, 200, "\"passo\":true");
            verdade("corpo JSON repassado", "{\"frente\":1,\"ticks\":5}".equals(ultimoCorpoJson));

            r = post(http, base + "/passo", "{\"invalido\":1}");
            confere("IllegalArgumentException vira 400", r, 400, "ação inválida");

            r = post(http, base + "/passo", "{\"conflito\":1}");
            confere("IllegalStateException vira 409", r, 409, "Entidade morta");

            r = post(http, base + "/passo", "{\"naoexiste\":1}");
            confere("ErroApi 404 preservado", r, 404, "Nenhum corpo");

            long t0 = System.nanoTime();
            r = post(http, base + "/passo", "{\"trava\":1}");
            long ms = (System.nanoTime() - t0) / 1_000_000;
            confere("timeout vira 504", r, 504, "F3+P");
            verdade("timeout respeita o limite (" + ms + " ms)", ms >= 350 && ms < 3000);

            r = post(http, base + "/parar", "");
            confere("POST /parar", r, 200, "\"ok\":true");

            r = post(http, base + "/reset", "{\"x\":1}");
            confere("POST /reset", r, 200, "\"reset\":true");

            r = get(http, base + "/passo");
            confere("GET em rota POST", r, 405, "Método não permitido");

            r = get(http, base + "/nada");
            confere("rota inexistente", r, 404, "Rota inexistente");

            String grande = "x".repeat(ApiServidor.TAMANHO_MAXIMO_CORPO + 1);
            r = post(http, base + "/passo", grande);
            confere("corpo grande demais", r, 400, "acima de");

        }

        if (falhas > 0) {
            System.err.println(falhas + " falha(s)");
            System.exit(1);
        }
        System.out.println("ApiServidorTest: todos os testes passaram");
    }

    private record Resp(int status, String corpo) { }

    private static Resp get(HttpClient http, String url) throws Exception {
        HttpResponse<String> r = http.send(HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        return new Resp(r.statusCode(), r.body());
    }

    private static Resp post(HttpClient http, String url, String corpo) throws Exception {
        HttpResponse<String> r = http.send(HttpRequest.newBuilder(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(corpo)).build(), HttpResponse.BodyHandlers.ofString());
        return new Resp(r.statusCode(), r.body());
    }

    private static void confere(String nome, Resp r, int status, String trecho) {
        boolean ok = r.status() == status && r.corpo().contains(trecho);
        if (!ok) System.err.println("FALHOU: " + nome + " -> " + r.status() + " " + r.corpo());
        else System.out.println("ok: " + nome);
        if (!ok) falhas++;
    }

    private static void verdade(String nome, boolean ok) {
        if (!ok) { System.err.println("FALHOU: " + nome); falhas++; }
        else System.out.println("ok: " + nome);
    }
}
