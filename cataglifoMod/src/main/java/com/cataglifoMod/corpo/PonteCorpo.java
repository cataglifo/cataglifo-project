package com.cataglifoMod.corpo;

import java.util.concurrent.CompletableFuture;

/**
 * Contrato entre a camada HTTP e o corpo no jogo. Não depende de Minecraft nem de Gson,
 * para que o servidor HTTP possa ser testado fora do jogo.
 *
 * Todos os métodos devolvem JSON como texto e NÃO podem bloquear a thread chamadora:
 * o trabalho no mundo precisa ser agendado na thread do servidor.
 */
interface PonteCorpo {

    /** Lista de corpos conhecidos: {"ok":true,"api":1,"corpos":[{"id":1,"vivo":true}]}. */
    String saude();

    /** Observação atual. {@code corpo} é o id da entidade, ou null para o primeiro corpo vivo. */
    CompletableFuture<String> observar(Integer corpo);

    /** Aplica a ação descrita em {@code jsonAcao} e responde com a observação ao fim dos ticks. */
    CompletableFuture<String> passo(Integer corpo, String jsonAcao);

    /** Cancela a ação em andamento. */
    CompletableFuture<String> parar(Integer corpo);

    /** Reposiciona o corpo ({@code jsonReset} pode ser vazio) e responde com a observação. */
    CompletableFuture<String> reiniciar(Integer corpo, String jsonReset);

    /** Erro com status HTTP definido (404 corpo inexistente, 409 passo em andamento, etc.). */
    final class ErroApi extends RuntimeException {
        final int status;
        ErroApi(int status, String mensagem) {
            super(mensagem);
            this.status = status;
        }
    }
}
