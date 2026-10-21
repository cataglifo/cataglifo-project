import json
import numpy as np
import pytest
from cataglifo.environment import GridWorld
from cataglifo.dqn import DQNAgent
from cataglifo.bridge import decision

def make_map(tmp_path, grid, max_steps=20):
    p = tmp_path / 'map.json'
    p.write_text(json.dumps({'grid': grid, 'max_steps': max_steps}), encoding='utf-8')
    return p

def test_reset_and_wall(tmp_path):
    env = GridWorld(make_map(tmp_path, ['#####', '#S.G#', '#####']))
    assert env.state().shape == (10,)
    _, reward, term, trunc, info = env.step(0)
    assert info['blocked'] and reward < 0 and not term and not trunc

def test_goal(tmp_path):
    env = GridWorld(make_map(tmp_path, ['#####', '#SG.#', '#####']))
    _, reward, term, trunc, info = env.step(3)
    assert term and not trunc and info['goal'] and reward > 0

def test_truncation(tmp_path):
    env = GridWorld(make_map(tmp_path, ['#####', '#S.G#', '#####'], max_steps=1))
    _, _, term, trunc, _ = env.step(0)
    assert trunc and not term

def test_training_and_checkpoint(tmp_path):
    agent = DQNAgent()
    state = np.zeros(10, dtype=np.float32)
    for _ in range(4):
        agent.buffer.add(state, 1, 1.0, state, False)
    assert agent.learn(batch_size=4) is not None
    path = tmp_path / 'model.pt'
    agent.save(path)
    restored = DQNAgent()
    restored.load(path)
    assert 0 <= restored.act(state) <= 3
    assert decision(restored, {'state': state.tolist()})['action'] in range(4)

def test_invalid_map(tmp_path):
    with pytest.raises(ValueError):
        GridWorld(make_map(tmp_path, ['###', '#S#']))
