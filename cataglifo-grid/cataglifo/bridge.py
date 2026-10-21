"""Contrato de integração futura com Forge, independente do transporte."""
import numpy as np
from .environment import ACTION_NAMES

def decision(agent, payload):
    """Recebe {'state': [10 floats]} e retorna {'action': int, 'name': str}.

    Não implementa comunicação de rede nem coleta de dados do Minecraft.
    """
    state = np.asarray(payload['state'], dtype=np.float32)
    if state.shape != (10,) or not np.isfinite(state).all():
        raise ValueError('Estado precisa ter 10 números finitos')
    action = agent.act(state)
    return {'action': action, 'name': ACTION_NAMES[action]}
