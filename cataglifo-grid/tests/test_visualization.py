import json
import random

from cataglifo.dqn import DQNAgent
from cataglifo.environment import GridWorld
from cataglifo.visualization import FrameData, TerminalVisualizer, map_lines, render_text
from visualize import run_episode


def make_map(tmp_path, grid, max_steps=10):
    path = tmp_path / "map.json"
    path.write_text(json.dumps({"grid": grid, "max_steps": max_steps}), encoding="utf-8")
    return path


def test_map_shows_walls_start_goal_agent_and_path(tmp_path):
    env = GridWorld(make_map(tmp_path, ["#####", "#S.G#", "#####"]))
    assert map_lines(env) == ["#####", "#A.G#", "#####"]
    env.step(3)
    assert map_lines(env) == ["#####", "#SAG#", "#####"]
    text = render_text(env, FrameData(1, 1, 10, 0.95, action=3))
    assert "DIREITA →" in text
    assert "#SAG#" in text
    env.step(3)
    assert map_lines(env) == ["#####", "#S*A#", "#####"]


def test_episode_ends_at_goal_and_collects_real_metrics(tmp_path):
    env = GridWorld(make_map(tmp_path, ["#####", "#SG.#", "#####"]))
    result = run_episode(env, lambda _state: 3, TerminalVisualizer(enabled=False))
    assert result["success"] is True
    assert result["steps"] == 1
    assert result["visited"] == 2


def test_random_mode_uses_valid_actions_and_terminates(tmp_path):
    env = GridWorld(make_map(tmp_path, ["#####", "#S.G#", "#####"], max_steps=3))
    rng = random.Random(7)
    actions = []

    def choose(_state):
        action = rng.randrange(4)
        actions.append(action)
        return action

    result = run_episode(env, choose, TerminalVisualizer(enabled=False))
    assert actions and all(0 <= action < 4 for action in actions)
    assert result["steps"] <= env.max_steps


def test_saved_model_runs_visual_episode_without_interface(tmp_path):
    env = GridWorld(make_map(tmp_path, ["#####", "#S.G#", "#####"], max_steps=2))
    path = tmp_path / "model.pt"
    DQNAgent(state_size=len(env.state())).save(path)
    restored = DQNAgent(state_size=len(env.state()))
    restored.load(path)
    result = run_episode(env, lambda state: restored.act(state), TerminalVisualizer(enabled=False))
    assert result["steps"] <= 2
