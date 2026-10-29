# API local do corpo (para o agente externo)

O mod abre um servidor HTTP em `http://127.0.0.1:8765` (somente a própria máquina) quando
o primeiro corpo faz tick no servidor, ou seja, depois de `/summon cataglifo:corpo ~ ~ ~`.
O agente (Python) lê a observação, decide e manda a ação por ele.

Porta: `-Dcataglifo.api.porta=NNNN`. Desligar: `-Dcataglifo.api.desativada=true`.

## Rotas

Todas respondem JSON UTF-8. `?corpo=ID` escolhe a entidade (o `id` vem na observação);
sem ele vale o primeiro corpo vivo.

| Rota | Corpo da requisição | Resposta |
|---|---|---|
| `GET /saude` | – | `{"ok":true,"api":1,"corpos":[{"id":12,"vivo":true}]}` |
| `GET /observacao` | – | observação (`schemaVersion` 2) |
| `POST /passo` | `{"frente":1,"lado":0,"yaw":0,"pitch":0,"pular":false,"ticks":5}` | observação **depois** dos `ticks` |
| `POST /parar` | – | `{"ok":true}` |
| `POST /reset` | opcional: `{"x":0,"y":-60,"z":0,"yaw":0}` | observação após o reset |

- `/passo` só responde quando a ação termina (1 a 20 ticks; padrão 5), então cada chamada
  equivale a um passo de ambiente. Todos os campos são opcionais, mas o corpo JSON é obrigatório.
  Limites do mod: `frente` e `lado` em [-1, 1]; `yaw` e `pitch` em [-45, 45] por chamada.
- Um `/passo` novo substitui o anterior que ainda estiver pendente (o antigo recebe 409).
- `/reset` sem coordenadas leva o corpo ao ponto onde ele foi visto pela primeira vez.
- A observação ganhou os campos `id` e `tick`; o resto do esquema é o mesmo.

## Erros

`{"ok":false,"erro":"mensagem"}` com status 400 (entrada inválida), 404 (sem corpo ou rota),
405, 409 (corpo morto ou passo substituído), 504 (o jogo não respondeu em 10 s).

## Cuidados

- No single-player, o menu de pausa congela o servidor do jogo, e o `/passo` expira. Ao
  rodar o agente em outra janela, aperte **F3+P** no jogo para não pausar ao perder o foco.
- Quem toca no mundo é sempre a thread do servidor; a thread HTTP só espera o resultado.
- Se o corpo morrer, a API responde `"morto":true` e depois 409. Falta o respawn (TODO do `CorpoEntity`).
- Teste da camada HTTP sem abrir o jogo: `./gradlew apiServidorTest`.
