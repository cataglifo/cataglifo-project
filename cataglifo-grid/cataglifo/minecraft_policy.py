"""Política reativa de referência: só olha os raios, sem aprendizado.

Serve para provar o ciclo observar -> decidir -> agir e como linha de base para comparar com
o DQN. Os limiares abaixo são hipóteses a calibrar no jogo (o CORRECOES.md do mod avisa que
a detecção de buracos ainda precisa de calibração).
"""
import math
from .minecraft_state import (ACOES, ALCANCE, DESVIOS_PITCH, DESVIOS_YAW, eh_perigo,
                              indice_raio, validar)

ALTURA_OLHO = 1.53        # blocos; dist do raio a 30° em chão plano foi ~3.06 no jogo
FOLGA_PAREDE = 2.5        # blocos livres à frente para seguir em frente
TOLERANCIA_BURACO = 1.35  # distância medida / esperada acima disso = chão sumiu
I_CENTRO = DESVIOS_YAW.index(0.0)
FRENTE, ESQUERDA, DIREITA = ACOES.index('frente'), ACOES.index('girar_esquerda'), ACOES.index('girar_direita')


def _distancia(raio):
    return float(raio['distNormalizada']) * ALCANCE


def _chao_ok(obs, i_yaw):
    """False se, nessa direção, há perigo ou o chão a 30°/60° está longe demais (buraco)."""
    for i_pitch in (1, 2):  # raios apontados para baixo
        raio = obs['raios'][indice_raio(i_pitch, i_yaw)]
        if eh_perigo(raio):
            return False
        esperado = ALTURA_OLHO / math.sin(math.radians(DESVIOS_PITCH[i_pitch]))
        if not raio['acertou'] or _distancia(raio) > esperado * TOLERANCIA_BURACO:
            return False
    return True


def _folga(obs, i_yaw):
    """Quanto espaço há à frente nessa direção (0 se o chão não presta)."""
    horizontal = obs['raios'][indice_raio(0, i_yaw)]
    if not _chao_ok(obs, i_yaw) or eh_perigo(horizontal):
        return 0.0
    return _distancia(horizontal)


def escolher_acao(obs):
    """Devolve o índice de ACOES: segue em frente se livre, senão gira para o lado com mais folga.

    Pressupõe o corpo olhando na horizontal (pitch ~ 0), que é como o reset o deixa.
    """
    validar(obs)
    if abs(float(obs['pitch'])) > 5.0:
        raise ValueError('A política reativa pressupõe pitch próximo de 0; faça /reset antes')
    if _folga(obs, I_CENTRO) >= FOLGA_PAREDE:
        return FRENTE
    esquerda = max(_folga(obs, i) for i in range(I_CENTRO))
    direita = max(_folga(obs, i) for i in range(I_CENTRO + 1, len(DESVIOS_YAW)))
    return ESQUERDA if esquerda > direita else DIREITA
