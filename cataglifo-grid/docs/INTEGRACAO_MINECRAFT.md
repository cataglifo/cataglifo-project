# Integração com o mod do Minecraft

O mod (branch `modMine`, pasta `cataglifoMod/`, arquivo `API.md`) expõe o corpo por HTTP em
`http://127.0.0.1:8765`. Este pacote só usa a biblioteca padrão para falar com ele; `numpy`
é necessário para o vetor de estado e `torch` apenas se você usar um modelo DQN.

## Como rodar

1. Abra o jogo com o mod (`./gradlew runClient`), crie um mundo com comandos e rode
   `/summon cataglifo:corpo ~ ~ ~`. Aperte **F3+P** para o jogo não pausar ao perder o foco.
2. No terminal, dentro de `cataglifo-grid`:

```bash
python run_minecraft.py --passos 200
python run_minecraft.py --passos 200 --reset --log outputs/observacoes.jsonl
python run_minecraft.py --passos 200 --metricas-csv outputs/metricas.csv
```

## Arquivos

| Arquivo | Função |
|---|---|
| `cataglifo/minecraft_client.py` | Cliente HTTP: `observar()`, `passo(...)`, `parar()`, `reset(...)` |
| `cataglifo/minecraft_state.py` | Observação -> vetor de `TAMANHO_ESTADO` (33) números; índice da ação -> comando |
| `cataglifo/minecraft_policy.py` | Política reativa de referência (sem aprendizado) |
| `cataglifo/minecraft_reward.py` | Pontuação de exploração, dano e morte; registro das regiões visitadas |
| `run_minecraft.py` | Laço observar -> decidir -> agir, com log opcional em JSON Lines |
| `tests/test_minecraft.py` | Testes sem jogo: estado, política e cliente contra um servidor falso |

## Estado e ações

Estado (33 floats em [0, 1]): 15 distâncias normalizadas dos raios, 15 marcadores de perigo
(lava, fogo, magma, cacto), `noChao`, `emLava` e `vida/20`. Ações: `frente`, `girar_esquerda`,
`girar_direita`, `pular_frente`. São diferentes do protótipo 2D (10 números, 4 direções):
**o modelo treinado no GridWorld não serve aqui**, e `DQNAgent.load` recusa o checkpoint.

## Recompensas e métricas

Cada região corresponde a uma área de 16 x 16 blocos, alinhada às chunks do Minecraft. A
região inicial já começa registrada. Depois de cada ação, a transição recebe:

| Evento | Recompensa |
|---|---:|
| Entrar em uma região ainda não visitada | `+1,00` |
| Deslocar-se dentro de uma região já visitada | `-0,10` |
| Executar qualquer ação | `-0,01` |
| Perder vida | `-1,00` por ponto |
| Morrer | `-10,00` |

Girar sem mudar X/Z não conta como revisita, mas ainda tem o custo de ação. A pontuação e o
número de regiões aparecem no terminal. `--metricas-csv` cria um arquivo novo com passo,
ação, recompensa, pontuação acumulada, região, posição, vida e estado de morte; ele pode ser
aberto em uma planilha para produzir gráficos. O arquivo usa ponto e vírgula como separador,
para que cada campo seja aberto em uma coluna nas configurações brasileiras do Excel e de
programas semelhantes. `--log` continua disponível separadamente
para registrar as observações completas em JSON Lines.

As recompensas são somente métricas nesta fase: elas não alteram a política reativa nem são
enviadas ao `DQNAgent`.

## Próximos passos para treinar de verdade

Falta um ambiente de treinamento com episódios que use `CorpoClient`, envie a recompensa ao
agente e trate o respawn no mod. A política
reativa serve de linha de base para comparar com o DQN. Os limiares dela (`FOLGA_PAREDE`,
`TOLERANCIA_BURACO`) são hipóteses e precisam ser calibrados no jogo.
