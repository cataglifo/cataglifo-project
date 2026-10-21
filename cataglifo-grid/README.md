# Cataglifo — Agente PyTorch (protótipo)

Agente Double DQN para exploração de um mapa controlado, com interface de decisão preparada para adaptação futura ao Minecraft/Forge.

## Estado do projeto e assistência de IA

Este repositório apresenta um **protótipo acadêmico inicial** de um agente Double DQN desenvolvido em PyTorch. Seu objetivo é validar conceitos de aprendizado por reforço em um ambiente 2D controlado, servindo como base para estudos e evoluções futuras.

O projeto foi desenvolvido com **assistência de ferramentas de inteligência artificial**, utilizadas no apoio à estruturação do código, documentação, testes e revisão de implementação. As decisões de arquitetura, a validação dos resultados e a responsabilidade pelo conteúdo permanecem com o autor.

Este protótipo ainda não representa um sistema pronto para produção e não foi validado em um ambiente real do Minecraft. Os resultados atuais estão limitados ao mapa e às condições incluídas no repositório.

## Requisitos

- Python 3.9 ou superior (Python 3.11 recomendado)
- Dependências listadas em `requirements.txt`

**Comece por [`docs/GUIA_IMPLEMENTACAO.md`](docs/GUIA_IMPLEMENTACAO.md)** para instalação, arquitetura, justificativas, comandos e explicação acadêmica.

```bash
python3 -m venv .venv
source .venv/bin/activate
python -m pip install -r requirements.txt
python -m pytest -q
python train.py --episodes 600
python evaluate.py --model outputs/final.pt
python visualize.py --model outputs/final.pt --speed 0.3
python visualize.py --random --speed 0.3
```

Consulte também [`docs/VISUALIZACAO_TERMINAL.md`](docs/VISUALIZACAO_TERMINAL.md) para animação, controles e treinamento visual.

**Escopo:** implementação Python; **não** inclui servidor de comunicação, mod Forge ou validação de desempenho no Minecraft.
