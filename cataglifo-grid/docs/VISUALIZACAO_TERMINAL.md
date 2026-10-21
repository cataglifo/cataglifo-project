# Visualização interativa no terminal

## Componentes

- `cataglifo/visualization.py`: converte o estado real de `GridWorld` em mapa e painel, mantém o caminho visitado e trata animação e teclado.
- `visualize.py`: executa um episódio com checkpoint ou política aleatória.
- `train.py`: aceita visualização opcional e amostragem de episódios.
- `tests/test_visualization.py`: testa mapa, movimento, caminho, término, aleatoriedade, checkpoint e modo sem interface.
- `requirements.txt`: inclui `rich`, usado para atualização estável do painel.

O renderizador apenas lê `grid`, `pos`, `visited`, `steps` e as informações retornadas por `GridWorld.step`. Movimento, colisões, recompensa e término continuam exclusivamente no ambiente; a política continua no `DQNAgent`. Assim, ativar a interface não muda o algoritmo nem o formato `.pt`.

Com `rich`, um painel vivo é atualizado no mesmo espaço do terminal para reduzir flicker. Sem `rich`, há um fallback textual. Em entrada interativa, pressione `p` ou espaço para pausar/continuar, `n` ou `s` para avançar um passo enquanto pausado e `q` para encerrar com segurança. Alguns consoles redirecionados ou IDEs não expõem teclas em tempo real; nesses casos somente a animação automática fica disponível.

## Instalação e execução

No macOS usando PowerShell, dentro do projeto:

```powershell
python3 -m pip install -r requirements.txt
```

Avaliação visual com um modelo treinado:

```powershell
python3 visualize.py --model outputs/final.pt --speed 0.3
```

Treinamento visual (todos os episódios):

```powershell
python3 train.py --episodes 600 --visualize --speed 0.1
```

Para visualizar somente um em cada 25 episódios (além do primeiro):

```powershell
python3 train.py --episodes 600 --visualize --visualize-every 25 --speed 0.1
```

Agente aleatório, com semente reproduzível:

```powershell
python3 visualize.py --random --seed 42 --speed 0.3
```

O treinamento tradicional permanece igual:

```powershell
python3 train.py --episodes 600
```

## Leitura do painel

`A` é a posição atual, `#` é obstáculo, `.` é célula livre ainda não visitada, `S` é a origem, `G` é o objetivo e `*` é uma posição visitada. A recompensa acumulada é a soma real do episódio; epsilon informa a chance de ação exploratória; posições únicas vêm de `env.visited`; colisões contam respostas `blocked` do ambiente. O status distingue execução, objetivo alcançado e término pelo limite de passos.

Para uma comparação qualitativa, execute o mesmo mapa e velocidade primeiro com `--model` e depois com `--random --seed 42`. Isso ajuda a observar trajetórias, mas não substitui comparação estatística de muitas execuções.

## Limitações e melhorias

- A política treinada é determinística com epsilon zero no mesmo mapa; repetições não criam cenários novos.
- Controles instantâneos dependem de um TTY. Redirecionamento de saída e alguns consoles de IDE podem não suportá-los.
- O fallback sem `rich` imprime quadros sequencialmente e pode gerar bastante saída.
- Uma evolução possível é salvar replays, criar mapas variados e calcular comparações agregadas entre políticas.
