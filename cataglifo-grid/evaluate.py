import argparse
from pathlib import Path
from cataglifo.environment import GridWorld, ACTION_NAMES
from cataglifo.dqn import DQNAgent

ROOT = Path(__file__).resolve().parent

def main():
    p = argparse.ArgumentParser()
    p.add_argument('--model', type=Path, default=ROOT / 'outputs/final.pt')
    p.add_argument('--map', type=Path, default=ROOT / 'config/mapa.json')
    p.add_argument('--episodes', type=int, default=20)
    p.add_argument('--verbose', action='store_true')
    args = p.parse_args()
    if args.episodes < 1:
        p.error('episodes deve ser positivo')
    env = GridWorld(args.map)
    agent = DQNAgent(state_size=len(env.state()))
    agent.load(args.model)
    successes = 0
    total_rewards = []
    visited = []
    for ep in range(args.episodes):
        state = env.reset()
        reward_sum = 0
        while True:
            action = agent.act(state, epsilon=0)
            state, reward, terminated, truncated, info = env.step(action)
            reward_sum += reward
            if args.verbose:
                print(f'ep={ep+1} ação={ACTION_NAMES[action]} pos={env.pos} recompensa={reward:.2f}')
            if terminated or truncated:
                break
        successes += int(info['goal'])
        total_rewards.append(reward_sum)
        visited.append(info['visited'])
    print(f'Taxa de sucesso: {successes / args.episodes:.1%}')
    print(f'Recompensa média: {sum(total_rewards) / args.episodes:.2f}')
    print(f'Células únicas médias: {sum(visited) / args.episodes:.2f}')

if __name__ == '__main__':
    main()
