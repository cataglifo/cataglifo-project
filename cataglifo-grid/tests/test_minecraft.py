import json
import threading
from http.server import BaseHTTPRequestHandler, HTTPServer

import numpy as np
import pytest

from cataglifo.minecraft_client import ApiIndisponivel, CorpoClient, ErroApi
from cataglifo.minecraft_policy import ALTURA_OLHO, escolher_acao
from cataglifo.minecraft_state import (ACOES, DESVIOS_PITCH, DESVIOS_YAW, N_RAIOS, TAMANHO_ESTADO,
                                       acao_para_passo, codificar_estado, indice_raio)
import math


def raio(dyaw, dpitch, dist, acertou=True, bloco='minecraft:grass_block', fluido='minecraft:empty'):
    return {'dyaw': dyaw, 'dpitch': dpitch, 'acertou': acertou, 'dist': dist,
            'distNormalizada': min(dist / 16.0, 1.0), 'bloco': bloco, 'fluido': fluido}


def obs_livre():
    """Chão plano, nada à frente: horizontais sem colisão, raios para baixo no chão esperado."""
    raios = []
    for dp in DESVIOS_PITCH:
        for dy in DESVIOS_YAW:
            if dp == 0:
                raios.append(raio(dy, dp, 16.0, acertou=False, bloco='minecraft:air'))
            else:
                raios.append(raio(dy, dp, ALTURA_OLHO / math.sin(math.radians(dp))))
    return {'schemaVersion': 2, 'id': 1, 'tick': 10, 'acaoAtiva': False, 'morto': False,
            'x': 0.0, 'y': -60.0, 'z': 0.0, 'vida': 20.0, 'noChao': True, 'emLava': False,
            'yaw': 0.0, 'pitch': 0.0, 'raios': raios}


def com_raio(obs, i_pitch, i_yaw, **novo):
    obs['raios'][indice_raio(i_pitch, i_yaw)].update(novo)
    return obs


def parede(obs, i_yaw, dist=1.0):
    return com_raio(obs, 0, i_yaw, acertou=True, dist=dist, distNormalizada=dist / 16.0,
                    bloco='minecraft:stone')


# ------------------------------------------------------------------ estado

def test_estado_tem_tamanho_e_faixa():
    estado = codificar_estado(obs_livre())
    assert estado.shape == (TAMANHO_ESTADO,) and estado.dtype == np.float32
    assert estado.min() >= 0.0 and estado.max() <= 1.0
    assert TAMANHO_ESTADO == 2 * N_RAIOS + 3


def test_estado_marca_perigo_e_chao():
    obs = com_raio(obs_livre(), 1, 2, bloco='minecraft:lava', fluido='minecraft:lava')
    obs['noChao'] = False
    estado = codificar_estado(obs)
    assert estado[N_RAIOS + indice_raio(1, 2)] == 1.0
    assert estado[N_RAIOS:2 * N_RAIOS].sum() == 1.0
    assert estado[2 * N_RAIOS] == 0.0  # noChao


def test_estado_rejeita_schema_e_ordem_diferentes():
    obs = obs_livre()
    obs['schemaVersion'] = 1
    with pytest.raises(ValueError):
        codificar_estado(obs)
    obs = obs_livre()
    obs['raios'][0], obs['raios'][1] = obs['raios'][1], obs['raios'][0]
    with pytest.raises(ValueError):
        codificar_estado(obs)
    obs = obs_livre()
    obs['raios'].pop()
    with pytest.raises(ValueError):
        codificar_estado(obs)


def test_acoes_viram_comandos_validos():
    assert acao_para_passo(0) == {'frente': 1.0, 'ticks': 5}
    assert acao_para_passo(ACOES.index('girar_direita'))['yaw'] > 0
    assert acao_para_passo(ACOES.index('girar_esquerda'))['yaw'] < 0
    assert acao_para_passo(ACOES.index('pular_frente'))['pular'] is True
    for i in range(len(ACOES)):
        assert abs(acao_para_passo(i).get('yaw', 0)) <= 45  # limite do mod
    for ruim in (-1, len(ACOES), 1.5, 'frente'):
        with pytest.raises(ValueError):
            acao_para_passo(ruim)


# ----------------------------------------------------------------- política

FRENTE, ESQ, DIR = ACOES.index('frente'), ACOES.index('girar_esquerda'), ACOES.index('girar_direita')


def test_politica_segue_em_frente_se_livre():
    assert escolher_acao(obs_livre()) == FRENTE


def test_politica_gira_para_o_lado_livre_diante_de_parede():
    obs = parede(obs_livre(), 2, 1.0)
    assert escolher_acao(parede(parede(obs, 0, 1.0), 1, 1.0)) == DIR   # esquerda bloqueada
    obs = parede(parede(parede(obs_livre(), 2, 1.0), 3, 1.0), 4, 1.0)
    assert escolher_acao(obs) == ESQ                                    # direita bloqueada


def test_politica_evita_buraco_e_lava():
    buraco = com_raio(obs_livre(), 1, 2, acertou=False, dist=16.0, distNormalizada=1.0, bloco='minecraft:air')
    assert escolher_acao(buraco) != FRENTE
    longe = com_raio(obs_livre(), 2, 2, dist=6.0, distNormalizada=6.0 / 16)  # chão bem mais longe: degrau fundo
    assert escolher_acao(longe) != FRENTE
    lava = com_raio(obs_livre(), 1, 2, bloco='minecraft:lava', fluido='minecraft:lava')
    assert escolher_acao(lava) != FRENTE


def test_politica_exige_pitch_perto_de_zero():
    obs = obs_livre()
    obs['pitch'] = 30.0
    with pytest.raises(ValueError):
        escolher_acao(obs)


# ------------------------------------------------------------------ cliente

class _Falso(BaseHTTPRequestHandler):
    recebidos = []

    def log_message(self, *args):
        pass

    def _responder(self, status, dados):
        corpo = json.dumps(dados).encode()
        self.send_response(status)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Content-Length', str(len(corpo)))
        self.end_headers()
        self.wfile.write(corpo)

    def do_GET(self):
        _Falso.recebidos.append(('GET', self.path, None))
        if self.path.startswith('/observacao'):
            self._responder(200, {'schemaVersion': 2, 'id': 3})
        else:
            self._responder(404, {'ok': False, 'erro': 'Rota inexistente'})

    def do_POST(self):
        dados = json.loads(self.rfile.read(int(self.headers['Content-Length'])) or b'{}')
        _Falso.recebidos.append(('POST', self.path, dados))
        if dados.get('ticks') == 99:
            self._responder(409, {'ok': False, 'erro': 'Entidade morta'})
        else:
            self._responder(200, {'eco': dados})


@pytest.fixture()
def servidor():
    _Falso.recebidos.clear()
    http = HTTPServer(('127.0.0.1', 0), _Falso)
    threading.Thread(target=http.serve_forever, daemon=True).start()
    yield f'http://127.0.0.1:{http.server_address[1]}'
    http.shutdown()


def test_cliente_observa_e_age(servidor):
    c = CorpoClient(servidor, corpo=3)
    assert c.observar()['id'] == 3
    assert c.passo(frente=1, ticks=5)['eco'] == {'frente': 1, 'lado': 0.0, 'yaw': 0.0, 'pitch': 0.0,
                                                  'pular': False, 'ticks': 5}
    assert _Falso.recebidos[0][1] == '/observacao?corpo=3'
    assert _Falso.recebidos[1][1] == '/passo?corpo=3'
    c.reset(x=1, y=2, z=3, yaw=90)
    assert _Falso.recebidos[-1][2] == {'x': 1, 'y': 2, 'z': 3, 'yaw': 90}


def test_cliente_traduz_erros(servidor):
    c = CorpoClient(servidor)
    with pytest.raises(ErroApi) as erro:
        c.passo(ticks=99)
    assert erro.value.status == 409 and 'morta' in erro.value.mensagem
    with pytest.raises(ApiIndisponivel):
        CorpoClient('http://127.0.0.1:9', timeout=1).observar()
