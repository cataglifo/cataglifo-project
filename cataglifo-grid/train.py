import argparse
import csv
import random
from pathlib import Path
import numpy as np
import torch
from cataglifo.environment import GridWorld
from cataglifo.dqn import DQNAgent
from cataglifo.visualization import FrameData, TerminalVisualizer, VisualizationStopped

ROOT = Path(__file__).resolve().parent

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--episodes', type=int, default=600)
    parser.add_argument('--seed', type=int, default=42)
    parser.add_argument('--batch-size', type=int, default=64)
    parser.add_argument('--map', type=Path, default=ROOT / 'config/mapa.json')
    parser.add_argument('--output', type=Path, default=ROOT / 'outputs')
    parser.add_argument('--visualize', action='store_true')
    parser.add_argument('--visualize-every', type=int, default=1,
                        help='visualiza um episódio a cada N episódios')
    parser.add_argument('--speed', type=float, default=0.1, help='segundos entre quadros')
    args = parser.parse_args()
    if args.episodes < 1 or args.batch_size < 1 or args.visualize_every < 1 or args.speed < 0:
        parser.error('episodes, batch-size e visualize-every devem ser positivos; speed não pode ser negativo')
    random.seed(args.seed)
    np.random.seed(args.seed)
    torch.manual_seed(args.seed)
    env = GridWorld(args.map)
    agent = DQNAgent(state_size=len(env.state()))
    args.output.mkdir(parents=True, exist_ok=True)
    rows = []
    best = float('-inf')
    stopped = False
    with TerminalVisualizer(args.speed, enabled=args.visualize) as visualizer:
        for ep in range(1, args.episodes + 1):
            state = env.reset()
            total = 0.0
            losses = []
            collisions = 0
            epsilon = max(0.05, 1.0 - (ep - 1) / max(1, args.episodes * 0.8))
            show_episode = args.visualize and (ep == 1 or ep % args.visualize_every == 0)
            try:
                if show_episode:
                    visualizer.update(env, FrameData(ep, 0, env.max_steps, total, epsilon=epsilon))
                while True:
                    action = agent.act(state, epsilon)
                    nxt, reward, terminated, truncated, info = env.step(action)
                    # Em limite de passos, o episódio termina para fins do alvo de treinamento.
                    done = terminated or truncated
                    agent.buffer.add(state, action, reward, nxt, done)
                    loss = agent.learn(args.batch_size)
                    if loss is not None:
                        losses.append(loss)
                    total += reward
                    state = nxt
                    collisions += int(info['blocked'])
                    if show_episode:
                        status = ('OBJETIVO ALCANÇADO' if terminated else
                                  ('EPISÓDIO ENCERRADO' if truncated else 'EM EXECUÇÃO'))
                        visualizer.update(
                            env, FrameData(ep, env.steps, env.max_steps, total, action,
                                           epsilon, collisions, status, info['blocked']), wait=not done)
                    if done:
                        break
            except VisualizationStopped:
                stopped = True
                print('\nVisualização encerrada; finalizando sem registrar o episódio incompleto.')
                break
            if stopped:
                break
            if ep % 10 == 0:
                agent.sync_target()
            rows.append({'episode': ep, 'reward': round(total, 4), 'steps': env.steps,
                         'visited': info['visited'], 'success': int(info['goal']),
                         'epsilon': round(epsilon, 4),
                         'loss': round(float(np.mean(losses)), 6) if losses else ''})
            if total > best:
                best = total
                agent.save(args.output / 'best.pt')
            if ep % 25 == 0 or ep == 1:
                print(f'Episódio {ep}/{args.episodes} | recompensa={total:.2f} | '
                      f'sucesso={info["goal"]} | epsilon={epsilon:.2f}')
    if not rows:
        print('Treinamento encerrado antes da conclusão do primeiro episódio; nenhum arquivo foi salvo.')
        return
    agent.save(args.output / 'final.pt')
    with (args.output / 'metrics.csv').open('w', newline='', encoding='utf-8') as f:
        writer = csv.DictWriter(f, fieldnames=rows[0].keys())
        writer.writeheader()
        writer.writerows(rows)
    print('Arquivos salvos em:', args.output)

if __name__ == '__main__':
    main()
