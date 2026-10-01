from enviroment import Ambiente, NOMES_ACOES

import random

acao = random.randint(0,3) # 0 = cima, 1 = baixo, 2 = esquerda, 3 = direita

ambiente = Ambiente()

estado = ambiente.reset()

for passo in range(20):
    acao = random.radint(0,3)
    
    novo_estado, recompensa, terminou = (
        ambiente.step(acao)
        )
    
    print(
    f"Estado: {estado} | "
    f"Ação: {NOMES_ACOES[acao]} | "
    f"Recompensa: {recompensa} | "
    f"Novo estado: {novo_estado}"
)
    
    
    estado = novo_estado
    
    if terminou:
        print('O agente chegou ao objetivo!')
        break
    
