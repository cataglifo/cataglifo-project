package com.cataglifoMod.corpo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Servidor HTTP mínimo (somente 127.0.0.1) que expõe o corpo para um agente externo.
 *
 * Usa só java.net.ServerSocket (módulo java.base) em vez de com.sun.net.httpserver,
 * para não depender de o JDK exportar esse módulo dentro do Forge.
 * As requisições rodam em threads próprias; quem toca no mundo é a {@link PonteCorpo}.
 *
 *   GET  /saude
 *   GET  /observacao[?corpo=ID]
 *   POST /passo[?corpo=ID]     corpo JSON: {"frente":1,"lado":0,"yaw":0,"pitch":0,"pular":false,"ticks":5}
 *   POST /parar[?corpo=ID]
 *   POST /reset[?corpo=ID]     corpo JSON opcional: {"x":0,"y":-60,"z":0,"yaw":0}
 */
final class ApiServidor implements AutoCloseable {
    static final int TAMANHO_MAXIMO_CORPO = 4096;

    private final PonteCorpo ponte;
    private final ServerSocket socket;
    private final ExecutorService trabalhadores;
    private final long limiteMs;
    private volatile boolean ativo = true;

    /** @param porta 0 escolhe uma porta livre (usado nos testes); @param limiteMs espera máxima por resposta */
    ApiServidor(PonteCorpo ponte, int porta, long limiteMs) throws IOException {
        this.ponte = ponte;
        this.limiteMs = limiteMs;
        // Só loopback: o corpo não deve ser controlável por outras máquinas da rede.
        this.socket = new ServerSocket(porta, 16, InetAddress.getByAddress(new byte[] {127, 0, 0, 1}));
        this.trabalhadores = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "cataglifo-api-worker");
            t.setDaemon(true); // nunca impede o jogo de fechar
            return t;
        });
        Thread aceitador = new Thread(this::aceitar, "cataglifo-api-accept");
        aceitador.setDaemon(true);
        aceitador.start();
    }

    int porta() { return socket.getLocalPort(); }

    @Override
    public void close() {
        ativo = false;
        try { socket.close(); } catch (IOException ignorado) { }
        trabalhadores.shutdownNow();
    }

    private void aceitar() {
        while (ativo) {
            try {
                Socket cliente = socket.accept();
                trabalhadores.execute(() -> atender(cliente));
            } catch (IOException e) {
                if (!ativo) return;
            } catch (RuntimeException e) {
                return; // executor encerrado
            }
        }
    }

    // ---------------------------------------------------------------- HTTP

    private record Requisicao(String metodo, String caminho, Map<String, String> query, String corpo) { }

    private void atender(Socket cliente) {
        try (cliente) {
            cliente.setSoTimeout(5000);
            Requisicao req;
            try {
                req = ler(cliente.getInputStream());
            } catch (SocketTimeoutException e) {
                escrever(cliente.getOutputStream(), 408, erro("Tempo esgotado lendo a requisição"));
                return;
            } catch (IllegalArgumentException e) {
                escrever(cliente.getOutputStream(), 400, erro(e.getMessage()));
                return;
            }
            String[] resposta = rotear(req);
            escrever(cliente.getOutputStream(), Integer.parseInt(resposta[0]), resposta[1]);
        } catch (IOException ignorado) {
            // cliente fechou a conexão; nada a fazer
        }
    }

    private String[] rotear(Requisicao req) {
        try {
            Integer corpo = idDoCorpo(req.query());
            String m = req.metodo();
            String c = req.caminho();
            if (c.equals("/saude") && m.equals("GET")) return ok(ponte.saude());
            if (c.equals("/observacao") && m.equals("GET")) return ok(esperar(ponte.observar(corpo)));
            if (c.equals("/passo") && m.equals("POST")) return ok(esperar(ponte.passo(corpo, req.corpo())));
            if (c.equals("/parar") && m.equals("POST")) return ok(esperar(ponte.parar(corpo)));
            if (c.equals("/reset") && m.equals("POST")) return ok(esperar(ponte.reiniciar(corpo, req.corpo())));
            boolean existe = c.equals("/saude") || c.equals("/observacao") || c.equals("/passo")
                    || c.equals("/parar") || c.equals("/reset");
            return existe ? falha(405, "Método não permitido: " + m + " " + c)
                          : falha(404, "Rota inexistente: " + c);
        } catch (PonteCorpo.ErroApi e) {
            return falha(e.status, e.getMessage());
        } catch (IllegalArgumentException e) {
            return falha(400, e.getMessage());
        } catch (RuntimeException e) {
            return falha(500, "Erro interno: " + e.getMessage());
        }
    }

    /** Espera a resposta sem prender a thread do jogo: só esta thread da API bloqueia. */
    private String esperar(CompletableFuture<String> futuro) {
        try {
            return futuro.get(limiteMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            futuro.cancel(true);
            throw new PonteCorpo.ErroApi(504, "O jogo não respondeu a tempo. Se estiver em mundo "
                    + "single-player, o menu de pausa congela o servidor (use F3+P).");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PonteCorpo.ErroApi(503, "Interrompido");
        } catch (ExecutionException e) {
            Throwable causa = e.getCause() == null ? e : e.getCause();
            if (causa instanceof PonteCorpo.ErroApi api) throw api;
            if (causa instanceof IllegalArgumentException arg) throw arg;
            if (causa instanceof IllegalStateException est) throw new PonteCorpo.ErroApi(409, est.getMessage());
            throw new PonteCorpo.ErroApi(500, String.valueOf(causa.getMessage()));
        }
    }

    private static Integer idDoCorpo(Map<String, String> query) {
        String v = query.get("corpo");
        if (v == null || v.isEmpty()) return null;
        try {
            return Integer.valueOf(v);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Parâmetro 'corpo' deve ser um inteiro");
        }
    }

    private static String[] ok(String json) { return new String[] {"200", json}; }

    private static String[] falha(int status, String mensagem) {
        return new String[] {String.valueOf(status), erro(mensagem)};
    }

    private static String erro(String mensagem) {
        StringBuilder sb = new StringBuilder("{\"ok\":false,\"erro\":\"");
        for (char ch : String.valueOf(mensagem).toCharArray()) {
            switch (ch) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> { }
                default -> { if (ch >= 0x20) sb.append(ch); }
            }
        }
        return sb.append("\"}").toString();
    }

    // ------------------------------------------------------- parsing HTTP

    private static Requisicao ler(InputStream in) throws IOException {
        ByteArrayOutputStream cabecalho = new ByteArrayOutputStream();
        int anterior3 = -1, anterior2 = -1, anterior1 = -1, atual;
        while ((atual = in.read()) != -1) {
            cabecalho.write(atual);
            if (anterior3 == '\r' && anterior2 == '\n' && anterior1 == '\r' && atual == '\n') break;
            anterior3 = anterior2; anterior2 = anterior1; anterior1 = atual;
            if (cabecalho.size() > 8192) throw new IllegalArgumentException("Cabeçalho grande demais");
        }
        String[] linhas = cabecalho.toString(StandardCharsets.ISO_8859_1).split("\r\n");
        if (linhas.length == 0 || linhas[0].isEmpty()) throw new IllegalArgumentException("Requisição vazia");
        String[] partes = linhas[0].split(" ");
        if (partes.length < 2) throw new IllegalArgumentException("Linha de requisição inválida");

        int tamanho = 0;
        for (int i = 1; i < linhas.length; i++) {
            int dp = linhas[i].indexOf(':');
            if (dp > 0 && linhas[i].substring(0, dp).trim().equalsIgnoreCase("content-length")) {
                try {
                    tamanho = Integer.parseInt(linhas[i].substring(dp + 1).trim());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Content-Length inválido");
                }
            }
        }
        if (tamanho < 0 || tamanho > TAMANHO_MAXIMO_CORPO) {
            throw new IllegalArgumentException("Corpo da requisição acima de " + TAMANHO_MAXIMO_CORPO + " bytes");
        }
        byte[] dados = in.readNBytes(tamanho);
        if (dados.length != tamanho) throw new IllegalArgumentException("Corpo incompleto");

        String alvo = partes[1];
        String caminho = alvo;
        Map<String, String> query = new HashMap<>();
        int q = alvo.indexOf('?');
        if (q >= 0) {
            caminho = alvo.substring(0, q);
            for (String par : alvo.substring(q + 1).split("&")) {
                if (par.isEmpty()) continue;
                int ig = par.indexOf('=');
                String k = URLDecoder.decode(ig < 0 ? par : par.substring(0, ig), StandardCharsets.UTF_8);
                String v = ig < 0 ? "" : URLDecoder.decode(par.substring(ig + 1), StandardCharsets.UTF_8);
                query.put(k, v);
            }
        }
        return new Requisicao(partes[0].toUpperCase(), caminho, query, new String(dados, StandardCharsets.UTF_8));
    }

    private static void escrever(OutputStream out, int status, String json) throws IOException {
        byte[] corpo = json.getBytes(StandardCharsets.UTF_8);
        String frase = switch (status) {
            case 200 -> "OK";
            case 400 -> "Bad Request";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 408 -> "Request Timeout";
            case 409 -> "Conflict";
            case 503 -> "Service Unavailable";
            case 504 -> "Gateway Timeout";
            default -> "Internal Server Error";
        };
        String cab = "HTTP/1.1 " + status + " " + frase + "\r\n"
                + "Content-Type: application/json; charset=utf-8\r\n"
                + "Content-Length: " + corpo.length + "\r\n"
                + "Connection: close\r\n\r\n";
        out.write(cab.getBytes(StandardCharsets.ISO_8859_1));
        out.write(corpo);
        out.flush();
    }
}
