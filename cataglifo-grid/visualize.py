import argparse
import random
from pathlib import Path

from cataglifo.dqn import DQNAgent
from cataglifo.environment import GridWorld
from cataglifo.visualization import FrameData, TerminalVisualizer, VisualizationStopped

ROOT = Path(__file__).resolve().parent


def run_episode(env, choose_action, visualizer, episode=1, epsilon=None):
    state = env.reset()
    total = 0.0
    collisions = 0
    info = {"goal": False}
    visualizer.update(env, FrameData(episode, 0, env.max_steps, total, epsilon=epsilon))
    while True:
        action = choose_action(state)
        state, reward, terminated, truncated, info = env.step(action)
        total += reward
        collisions += int(info["blocked"])
        done = terminated or truncated
        status = "OBJETIVO ALCANÇADO" if terminated else ("EPISÓDIO ENCERRADO" if truncated else "EM EXECUÇÃO")
        visualizer.update(env, FrameData(episode, env.steps, env.max_steps, total, action,
                                         epsilon, collisions, status, info["blocked"]), wait=not done)
        if done:
            return {"reward": total, "steps": env.steps, "visited": info["visited"],
                    "collisions": collisions, "success": info["goal"]}


def main():
    parser = argparse.ArgumentParser(description="Visualiza o agente Cataglifo no terminal")
    policy = parser.add_mutually_exclusive_group(required=True)
    policy.add_argument("--model", type=Path, help="checkpoint .pt do agente")
    policy.add_argument("--random", action="store_true", help="usa ações aleatórias")
    parser.add_argument("--map", type=Path, default=ROOT / "config/mapa.json")
    parser.add_argument("--speed", type=float, default=0.3, help="segundos entre movimentos")
    parser.add_argument("--seed", type=int, default=42)
    args = parser.parse_args()
    if args.speed < 0:
        parser.error("speed não pode ser negativo")

    random.seed(args.seed)
    env = GridWorld(args.map)
    if args.random:
        choose_action = lambda _state: random.randrange(4)
        epsilon = 1.0
    else:
        agent = DQNAgent(state_size=len(env.state()))
        agent.load(args.model)
        choose_action = lambda state: agent.act(state, epsilon=0.0)
        epsilon = 0.0

    try:
        with TerminalVisualizer(args.speed) as visualizer:
            result = run_episode(env, choose_action, visualizer, epsilon=epsilon)
    except VisualizationStopped:
        print("Visualização encerrada pelo usuário; nenhum modelo foi alterado.")
        return
    print(f"Resultado: sucesso={result['success']} | recompensa={result['reward']:.2f} | "
          f"passos={result['steps']} | colisões={result['collisions']}")


if __name__ == "__main__":
    main()
