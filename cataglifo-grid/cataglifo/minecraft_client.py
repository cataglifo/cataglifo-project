"""Cliente HTTP da API do mod Cataglifo (só biblioteca padrão do Python).

O mod abre http://127.0.0.1:8765 quando o primeiro corpo existe no jogo
(/summon cataglifo:corpo ~ ~ ~). Veja docs/INTEGRACAO_MINECRAFT.md.
"""
import json
import urllib.error
import urllib.request

URL_PADRAO = 'http://127.0.0.1:8765'


class ErroApi(RuntimeError):
    """A API respondeu com erro (status HTTP e mensagem do mod)."""

    def __init__(self, status, mensagem):
        super().__init__(f'HTTP {status}: {mensagem}')
        self.status = status
        self.mensagem = mensagem


class ApiIndisponivel(ConnectionError):
    """Ninguém atendeu na URL: o jogo está fechado ou ainda não há corpo."""


class CorpoClient:
    def __init__(self, url=URL_PADRAO, corpo=None, timeout=15.0):
        self.url = url.rstrip('/')
        self.corpo = corpo      # id da entidade; None usa o primeiro corpo vivo
        self.timeout = timeout

    # ---------------------------------------------------------------- rotas

    def saude(self):
        return self._pedir('GET', '/saude')

    def observar(self):
        """Observação atual (JSON schemaVersion=2 do mod)."""
        return self._pedir('GET', '/observacao', com_corpo=True)

    def passo(self, frente=0.0, lado=0.0, yaw=0.0, pitch=0.0, pular=False, ticks=5):
        """Executa a ação por `ticks` ticks do jogo e devolve a observação ao final."""
        acao = {'frente': frente, 'lado': lado, 'yaw': yaw, 'pitch': pitch,
                'pular': bool(pular), 'ticks': int(ticks)}
        return self._pedir('POST', '/passo', acao, com_corpo=True)

    def parar(self):
        return self._pedir('POST', '/parar', {}, com_corpo=True)

    def reset(self, x=None, y=None, z=None, yaw=None):
        """Sem argumentos volta ao ponto onde o corpo foi visto pela primeira vez."""
        dados = {}
        if x is not None or y is not None or z is not None:
            dados.update({'x': x, 'y': y, 'z': z})
        if yaw is not None:
            dados['yaw'] = yaw
        return self._pedir('POST', '/reset', dados, com_corpo=True)

    # ------------------------------------------------------------- interno

    def _pedir(self, metodo, caminho, dados=None, com_corpo=False):
        url = self.url + caminho
        if com_corpo and self.corpo is not None:
            url += f'?corpo={int(self.corpo)}'
        corpo = None
        cabecalhos = {'Accept': 'application/json'}
        if metodo == 'POST':
            corpo = json.dumps(dados if dados is not None else {}).encode('utf-8')
            cabecalhos['Content-Type'] = 'application/json'
        req = urllib.request.Request(url, data=corpo, method=metodo, headers=cabecalhos)
        try:
            with urllib.request.urlopen(req, timeout=self.timeout) as resp:
                return json.loads(resp.read().decode('utf-8'))
        except urllib.error.HTTPError as e:
            texto = e.read().decode('utf-8', errors='replace')
            try:
                mensagem = json.loads(texto).get('erro', texto)
            except ValueError:
                mensagem = texto
            raise ErroApi(e.code, mensagem) from None
        except urllib.error.URLError as e:
            raise ApiIndisponivel(
                f'Sem resposta em {self.url} ({e.reason}). O jogo está aberto e o corpo foi '
                f'invocado com /summon cataglifo:corpo ~ ~ ~ ?') from None
