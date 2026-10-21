import json
from pathlib import Path
import numpy as np

ACTIONS = ((-1, 0), (1, 0), (0, -1), (0, 1))
ACTION_NAMES = ('cima', 'baixo', 'esquerda', 'direita')

class GridWorld:
    def __init__(self, path):
        cfg = json.loads(Path(path).read_text(encoding='utf-8'))
        self.grid = cfg['grid']
        if not self.grid or len({len(row) for row in self.grid}) != 1:
            raise ValueError('Mapa vazio ou com linhas de tamanhos diferentes')
        if sum(row.count('S') for row in self.grid) != 1 or sum(row.count('G') for row in self.grid) != 1:
            raise ValueError('Mapa precisa de exatamente um S e um G')
        if any(ch not in '#.SG' for row in self.grid for ch in row):
            raise ValueError('Caracter inválido no mapa')
        self.start = next((r, c) for r, row in enumerate(self.grid) for c, ch in enumerate(row) if ch == 'S')
        self.goal = next((r, c) for r, row in enumerate(self.grid) for c, ch in enumerate(row) if ch == 'G')
        self.max_steps = int(cfg.get('max_steps', 200))
        if self.max_steps < 1:
            raise ValueError('max_steps precisa ser positivo')
        self.reset()

    def reset(self):
        self.pos = self.start
        self.steps = 0
        self.visited = {self.pos}
        return self.state()

    def state(self):
        r, c = self.pos
        rows, cols = len(self.grid), len(self.grid[0])
        walls = []
        for dr, dc in ACTIONS:
            nr, nc = r + dr, c + dc
            walls.append(float(not (0 <= nr < rows and 0 <= nc < cols) or self.grid[nr][nc] == '#'))
        return np.array([r / max(rows-1, 1), c / max(cols-1, 1),
                         self.goal[0] / max(rows-1, 1), self.goal[1] / max(cols-1, 1),
                         *walls, len(self.visited) / (rows * cols), self.steps / self.max_steps], dtype=np.float32)

    def step(self, action):
        if not isinstance(action, (int, np.integer)) or not 0 <= action < len(ACTIONS):
            raise ValueError('Ação deve ser um inteiro entre 0 e 3')
        self.steps += 1
        dr, dc = ACTIONS[action]
        nr, nc = self.pos[0] + dr, self.pos[1] + dc
        blocked = not (0 <= nr < len(self.grid) and 0 <= nc < len(self.grid[0])) or self.grid[nr][nc] == '#'
        reward = -0.05
        if blocked:
            reward -= 0.5
        else:
            self.pos = (nr, nc)
            if self.pos in self.visited:
                reward -= 0.1
            else:
                reward += 1.0
                self.visited.add(self.pos)
        reached_goal = self.pos == self.goal
        if reached_goal:
            reward += 10.0
        terminated = reached_goal
        truncated = self.steps >= self.max_steps and not terminated
        return self.state(), reward, terminated, truncated, {'blocked': blocked, 'visited': len(self.visited), 'goal': reached_goal}
