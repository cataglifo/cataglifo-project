"""Recompensas e métricas de exploração para execuções no Minecraft.

Este módulo apenas avalia o resultado das ações. Ele não escolhe ações e ainda não
alimenta o treinamento DQN.
"""
import math
from dataclasses import dataclass


TAMANHO_REGIAO = 16.0
RECOMPENSA_REGIAO_NOVA = 1.0
PENALIDADE_REVISITA = -0.1
CUSTO_ACAO = -0.01
PENALIDADE_DANO = -1.0
PENALIDADE_MORTE = -10.0


@dataclass(frozen=True)
class ResultadoRecompensa:
    """Detalhamento da recompensa calculada após uma ação."""

    total: float
    exploracao: float
    custo_acao: float
    dano: float
    morte: float
    regiao: tuple
    regiao_nova: bool
    houve_deslocamento: bool
    pontos_vida_perdidos: float
    total_regioes: int


class RecompensaMinecraft:
    """Mantém as regiões visitadas e calcula a recompensa de cada transição."""

    def __init__(self, observacao_inicial, tamanho_regiao=TAMANHO_REGIAO):
        if tamanho_regiao <= 0:
            raise ValueError('tamanho_regiao deve ser positivo')
        self.tamanho_regiao = float(tamanho_regiao)
        self.regioes_visitadas = {self.regiao_de(observacao_inicial)}
        self.pontuacao = 0.0

    def regiao_de(self, observacao):
        """Converte a posição X/Z em uma região quadrada, inclusive no eixo negativo."""
        x = float(observacao['x'])
        z = float(observacao['z'])
        if not math.isfinite(x) or not math.isfinite(z):
            raise ValueError('A posição da observação deve ser finita')
        return (math.floor(x / self.tamanho_regiao), math.floor(z / self.tamanho_regiao))

    def calcular(self, anterior, atual):
        """Avalia uma transição e atualiza pontuação e regiões visitadas."""
        x_anterior, z_anterior = float(anterior['x']), float(anterior['z'])
        x_atual, z_atual = float(atual['x']), float(atual['z'])
        deslocou = not (math.isclose(x_anterior, x_atual, abs_tol=1e-6) and
                        math.isclose(z_anterior, z_atual, abs_tol=1e-6))

        regiao = self.regiao_de(atual)
        regiao_nova = deslocou and regiao not in self.regioes_visitadas
        exploracao = 0.0
        if deslocou:
            exploracao = RECOMPENSA_REGIAO_NOVA if regiao_nova else PENALIDADE_REVISITA
            self.regioes_visitadas.add(regiao)

        vida_anterior = float(anterior['vida'])
        vida_atual = float(atual['vida'])
        pontos_perdidos = max(0.0, vida_anterior - vida_atual)
        dano = PENALIDADE_DANO * pontos_perdidos
        morreu_agora = bool(atual.get('morto')) and not bool(anterior.get('morto'))
        morte = PENALIDADE_MORTE if morreu_agora else 0.0

        total = CUSTO_ACAO + exploracao + dano + morte
        self.pontuacao += total
        return ResultadoRecompensa(
            total=total,
            exploracao=exploracao,
            custo_acao=CUSTO_ACAO,
            dano=dano,
            morte=morte,
            regiao=regiao,
            regiao_nova=regiao_nova,
            houve_deslocamento=deslocou,
            pontos_vida_perdidos=pontos_perdidos,
            total_regioes=len(self.regioes_visitadas),
        )
