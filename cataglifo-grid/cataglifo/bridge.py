"""Contrato de integração futura com Forge, independente do transporte."""
import numpy as np
from .environment import ACTION_NAMES

def decision(agent, payload):
    """Recebe {'state': [floats]} e retorna {'action': int, 'name': str}.

    O tamanho do estado deve ser o mesmo da entrada da rede do agente
    (depende do tamanho do mapa; veja GridWorld.state).

    Não implementa comunicação de rede nem coleta de dados do Minecraft.
    """
    state = np.asarray(payload['state'], dtype=np.float32)
    expected = agent.online.layers[0].in_features
    if state.shape != (expected,) or not np.isfinite(state).all():
        raise ValueError(f'Estado precisa ter {expected} números finitos')
    action = agent.act(state)
    return {'action': action, 'name': ACTION_NAMES[action]}
