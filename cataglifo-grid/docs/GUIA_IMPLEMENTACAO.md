# Cataglifo — Guia de implementação do agente PyTorch

## 1. Objetivo

Implementar um protótipo de agente de **aprendizado por reforço profundo** (Double DQN) em um mapa 2D controlado, preparando uma interface conceitual para integração posterior com **Minecraft Java Edition + Forge**. Este pacote não implementa o mod Java, comunicação TCP/HTTP nem um ambiente 3D.

## 2. Arquitetura

```text
Mapa JSON -> GridWorld -> vetor de estado (10 números)
                          -> DQNAgent -> ação (0..3)
                          -> GridWorld.step -> recompensa, próximo estado, término
                          -> ReplayBuffer -> treino da rede online
                                             rede-alvo (target)
```

Na futura integração, o Forge deverá fornecer um vetor compatível ou será necessário redesenhar o estado e treinar novamente. O arquivo `bridge.py` é apenas o **contrato em memória**, não um servidor de comunicação.

## 3. Arquivos e justificativas

| Arquivo | O que faz | Por quê |
|---|---|---|
| `config/mapa.json` | Mapa, obstáculos e limite de passos | Reproduz experimentos com condições controladas |
| `cataglifo/environment.py` | Simula ações, estados e recompensas | Separa regras do ambiente da IA |
| `cataglifo/dqn.py` | Rede neural, memória, Double DQN e checkpoints | Implementa a política de aprendizado |
| `cataglifo/bridge.py` | Valida vetor de entrada e retorna ação | Delimita o futuro contrato Java ↔ Python |
| `train.py` | Executa episódios e grava métricas/modelos | Automatiza experimentos |
| `evaluate.py` | Mede o agente sem exploração aleatória | Distingue treinamento de avaliação |
| `tests/test_agent.py` | Testes de regras, rede e persistência | Detecta regressões antes da integração |
| `requirements.txt` | Dependências | Facilita instalação reprodutível |

## 4. Estado e ações

O estado é um vetor de **10 números float32**, nesta ordem:

1. linha normalizada;
2. coluna normalizada;
3. linha do objetivo normalizada;
4. coluna do objetivo normalizada;
5. parede acima (0/1);
6. parede abaixo (0/1);
7. parede à esquerda (0/1);
8. parede à direita (0/1);
9. proporção de células únicas visitadas em relação à área total;
10. fração do limite de passos consumida.

Ações: `0=cima`, `1=baixo`, `2=esquerda`, `3=direita`. **Esses valores são do protótipo 2D e não representam ainda comandos reais do Minecraft.**

Recompensas: passo `-0.05`; colisão `-0.5` adicional; revisita `-0.1` adicional; primeira visita `+1.0` adicional; objetivo `+10.0` adicional. Esses números são **hipóteses iniciais**, não parâmetros empiricamente validados. A recompensa de novas células pode incentivar exploração mesmo sem alcançar o objetivo; avaliar ambas as métricas.

## 5. Como instalar

No terminal, dentro da pasta `cataglifo_agent`:

```bash
python -m venv .venv
```

No Windows PowerShell:

```powershell
.\.venv\Scripts\Activate.ps1
python -m pip install -r requirements.txt
```

No Linux/macOS:

```bash
source .venv/bin/activate
python -m pip install -r requirements.txt
```

Requer Python compatível com a versão do PyTorch instalada (recomendado Python 3.11). Para instalação com GPU, consulte as instruções oficiais de PyTorch para a combinação de CUDA e sistema operacional.

## 6. Como testar

**Etapa A — testes automatizados:**

```bash
python -m pytest -q
```

**Etapa B — teste rápido de execução:**

```bash
python train.py --episodes 10
```

**Etapa C — treinamento inicial (não garante convergência):**

```bash
python train.py --episodes 600
```

**Etapa D — avaliação:**

```bash
python evaluate.py --model outputs/final.pt --episodes 20
```

Para observar cada ação:

```bash
python evaluate.py --model outputs/final.pt --episodes 1 --verbose
```

O treinamento cria `outputs/final.pt`, `outputs/best.pt` e `outputs/metrics.csv`. O checkpoint `best.pt` é escolhido pela **maior recompensa de um episódio de treino**, não por avaliação independente: não o considere automaticamente o melhor modelo generalizável.

## 7. Como explicar na apresentação do TCC

> Nosso agente utiliza aprendizado por reforço profundo. Em cada passo, o ambiente fornece dez características numéricas. Uma rede neural estima o valor esperado de quatro ações. O agente seleciona uma ação, recebe uma recompensa e armazena a experiência. Durante o treinamento, amostras dessas experiências atualizam a rede. Utilizamos uma rede-alvo para estabilizar as estimativas e Double DQN para reduzir a superestimação dos valores de ação. Primeiro validamos o método em um mapa controlado; depois pretendemos adaptar o estado, as ações e o transporte de mensagens para o mod Forge.

**Fórmula do alvo:** `y = r + gamma * (1 - done) * Q_target(s', argmax_a Q_online(s', a))`.

- `r`: recompensa recebida;
- `gamma`: desconto de recompensas futuras (`0.99`);
- `done`: episódio encerrado;
- `Q_online`: rede treinada por gradiente;
- `Q_target`: cópia periodicamente atualizada para calcular o alvo.

**Epsilon-greedy:** no começo o agente escolhe muitas ações aleatórias (`epsilon` alto); gradualmente explora menos (`epsilon` até `0.05`) e passa a usar mais a rede.

## 8. Como interpretar os resultados

Abra `outputs/metrics.csv`. Compare a taxa de sucesso (coluna `success`), recompensa por episódio, número de passos e quantidade de células únicas visitadas. A `loss` mede o erro do ajuste da rede, **não** a qualidade de navegação diretamente.

Execute avaliações em mapas diferentes para verificar generalização. O protótipo não possui política de avaliação aleatória com múltiplas sementes automatizada; isso é trabalho de validação pendente.

## 9. Pendências antes de afirmar que está pronto para Minecraft

1. Executar testes e corrigir eventuais falhas de dependência/plataforma.
2. Treinar e avaliar quantitativamente, com múltiplas sementes e comparação com agente aleatório.
3. Confirmar definição de sucesso: exploração/cobertura versus chegar ao objetivo; revisar recompensas.
4. Implementar percepção do mundo e execução de movimentos no mod Forge.
5. Definir transporte Java↔Python (por exemplo, TCP local), protocolo, mensagens, timeouts e sincronização de ticks.
6. Adaptar vetor de estado e espaço de ações para o ambiente 3D, retreinar e testar.
7. Validar checkpoints e versões do modelo antes da demonstração.

## 10. Limitações conhecidas

- A avaliação é determinística no mesmo mapa e modelo, portanto 20 episódios repetidos não equivalem a 20 cenários independentes.
- O treino trata `truncated` como terminal no alvo TD; essa é uma simplificação que pode ser revista.
- O estado não registra todas as células visitadas individualmente, apenas a contagem agregada; isso pode limitar a capacidade de evitar revisitas.
- O protótipo usa CPU por padrão. A seleção de GPU ainda não está automatizada.
- O código não foi comprovado em execução real só por ter sido gerado. Resultados dependem de teste e treinamento.
