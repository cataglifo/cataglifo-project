import torch


# Um tensor é uma matriz multidimensional que contém elementos de um único tipo de dados.
# Por exemplo, um tensor 2D é uma matriz, enquanto um tensor 3D é uma coleção de matrizes.
# Os tensores são a estrutura de dados fundamental do PyTorch e são usados para armazenar dados
# e realizar operações matemáticas.
print(torch.__version__)
print(torch.tensor([1, 2, 3]))


'''

VARIAVEIS DE ESTADO DO AGENTE E DO AMBIENTE:

[
    linha_agente,
    coluna_agente,
    linha_objetivo,
    coluna_objetivo
]

A grosso modo, um estado de agente é uma matriz. Sendo assim, o estado é representado por
Coluna x e Linha y do agente e Coluna x e Linha y do objetivo.

'''''


estado_agente = torch.tensor(
    [3.0, 1.0, 4.0, 4.0],
    dtype=torch.float32
)


## 0 = livre, 1 = obstáculo
MAPA = [
    [0, 0, 0, 1, 0],
    [0, 1, 0, 1, 0],
    [0, 1, 0, 0, 0],
    [0, 0, 1, 0, 0],
    [1, 0, 0, 0, 0],
]

# Declaração de ponto inicial e final do agente dentro da matriz do mapa#

inicio = (0,0)
fim = (4,4)

# Declaração de ações possíveis do agente dentro do mapa
NOMES_ACOES = {
    CIMA: "CIMA",
    BAIXO: "BAIXO",
    ESQUERDA: "ESQUERDA",
    DIREITA: "DIREITA",
}