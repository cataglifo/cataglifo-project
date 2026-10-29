"""Converte a observação do mod (schemaVersion=2) em vetor de estado e ações em comandos.

O estado do protótipo 2D tinha 10 números; este tem TAMANHO_ESTADO. Um checkpoint treinado
no GridWorld NÃO serve aqui: é preciso retreinar com este estado e este conjunto de ações.
"""
import math
import numpy as np

SCHEMA = 2
# Devem coincidir com DESVIOS_YAW / DESVIOS_PITCH de CorpoEntity.java (ordem: pitch, depois yaw).
DESVIOS_YAW = (-60.0, -30.0, 0.0, 30.0, 60.0)
DESVIOS_PITCH = (0.0, 30.0, 60.0)
N_RAIOS = len(DESVIOS_YAW) * len(DESVIOS_PITCH)
ALCANCE = 16.0                     # blocos; igual a ALCANCE no mod
VIDA_MAXIMA = 20.0
TAMANHO_ESTADO = 2 * N_RAIOS + 3   # 15 distâncias + 15 perigos + chão, lava, vida

PERIGOS = ('minecraft:lava', 'minecraft:fire', 'minecraft:soul_fire',
           'minecraft:magma_block', 'minecraft:cactus', 'minecraft:campfire')

ACOES = ('frente', 'girar_esquerda', 'girar_direita', 'pular_frente')
PASSO_GIRO = 30.0   # graus por ação de giro (o mod aceita no máximo 45)
TICKS_FRENTE = 5


def indice_raio(i_pitch, i_yaw):
    """Posição do raio na lista: 0..4 horizontais, 5..9 a 30° para baixo, 10..14 a 60°."""
    return i_pitch * len(DESVIOS_YAW) + i_yaw


def eh_perigo(raio):
    return raio.get('bloco') in PERIGOS or 'lava' in str(raio.get('fluido', ''))


def validar(obs):
    if obs.get('schemaVersion') != SCHEMA:
        raise ValueError(f"schemaVersion {obs.get('schemaVersion')!r} não suportado (esperado {SCHEMA})")
    raios = obs.get('raios')
    if not isinstance(raios, list) or len(raios) != N_RAIOS:
        raise ValueError(f'Esperados {N_RAIOS} raios')
    for i_p, d_p in enumerate(DESVIOS_PITCH):
        for i_y, d_y in enumerate(DESVIOS_YAW):
            r = raios[indice_raio(i_p, i_y)]
            if r.get('dpitch') != d_p or r.get('dyaw') != d_y:
                raise ValueError('Ordem dos raios diferente da esperada; o mod mudou?')
            if not math.isfinite(r['distNormalizada']):
                raise ValueError('distNormalizada não finita')


def codificar_estado(obs):
    """Vetor float32 de TAMANHO_ESTADO números, todos em [0, 1]."""
    validar(obs)
    dist = [min(max(float(r['distNormalizada']), 0.0), 1.0) for r in obs['raios']]
    perigo = [1.0 if eh_perigo(r) else 0.0 for r in obs['raios']]
    vida = min(max(float(obs['vida']) / VIDA_MAXIMA, 0.0), 1.0)
    return np.array(dist + perigo + [float(bool(obs['noChao'])), float(bool(obs['emLava'])), vida],
                    dtype=np.float32)


def acao_para_passo(indice):
    """Índice da ação -> argumentos de CorpoClient.passo()."""
    if not isinstance(indice, (int, np.integer)) or not 0 <= indice < len(ACOES):
        raise ValueError(f'Ação deve ser um inteiro entre 0 e {len(ACOES) - 1}')
    nome = ACOES[int(indice)]
    if nome == 'frente':
        return {'frente': 1.0, 'ticks': TICKS_FRENTE}
    if nome == 'girar_esquerda':
        return {'yaw': -PASSO_GIRO, 'ticks': 1}
    if nome == 'girar_direita':
        return {'yaw': PASSO_GIRO, 'ticks': 1}
    return {'frente': 1.0, 'pular': True, 'ticks': TICKS_FRENTE}
