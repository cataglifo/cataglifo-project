from collections import deque
import random
import numpy as np
import torch
from torch import nn

class QNetwork(nn.Module):
    def __init__(self, state_size=10, action_size=4):
        super().__init__()
        self.layers = nn.Sequential(nn.Linear(state_size, 128), nn.ReLU(), nn.Linear(128, 128), nn.ReLU(), nn.Linear(128, action_size))

    def forward(self, x):
        return self.layers(x)

class ReplayBuffer:
    def __init__(self, capacity=50000):
        self.memory = deque(maxlen=capacity)

    def add(self, state, action, reward, next_state, done):
        self.memory.append((state.copy(), action, reward, next_state.copy(), done))

    def sample(self, batch_size):
        return random.sample(self.memory, batch_size)

    def __len__(self):
        return len(self.memory)

class DQNAgent:
    def __init__(self, state_size=10, action_size=4, lr=0.001, gamma=0.99, device='cpu'):
        self.device = torch.device(device)
        self.online = QNetwork(state_size, action_size).to(self.device)
        self.target = QNetwork(state_size, action_size).to(self.device)
        self.target.load_state_dict(self.online.state_dict())
        self.target.eval()
        self.optimizer = torch.optim.Adam(self.online.parameters(), lr=lr)
        self.gamma = gamma
        self.action_size = action_size
        self.buffer = ReplayBuffer()
        self.loss_fn = nn.SmoothL1Loss()

    def act(self, state, epsilon=0.0):
        if random.random() < epsilon:
            return random.randrange(self.action_size)
        with torch.no_grad():
            values = self.online(torch.as_tensor(state, dtype=torch.float32, device=self.device).unsqueeze(0))
            return int(values.argmax(dim=1).item())

    def learn(self, batch_size=64):
        if len(self.buffer) < batch_size:
            return None
        samples = self.buffer.sample(batch_size)
        states, actions, rewards, next_states, dones = zip(*samples)
        states = torch.as_tensor(np.stack(states), dtype=torch.float32, device=self.device)
        next_states = torch.as_tensor(np.stack(next_states), dtype=torch.float32, device=self.device)
        actions = torch.as_tensor(actions, dtype=torch.long, device=self.device).unsqueeze(1)
        rewards = torch.as_tensor(rewards, dtype=torch.float32, device=self.device)
        dones = torch.as_tensor(dones, dtype=torch.float32, device=self.device)
        predicted = self.online(states).gather(1, actions).squeeze(1)
        with torch.no_grad():
            next_actions = self.online(next_states).argmax(dim=1, keepdim=True)
            next_q = self.target(next_states).gather(1, next_actions).squeeze(1)
            targets = rewards + self.gamma * next_q * (1 - dones)
        loss = self.loss_fn(predicted, targets)
        self.optimizer.zero_grad()
        loss.backward()
        nn.utils.clip_grad_norm_(self.online.parameters(), 10.0)
        self.optimizer.step()
        return float(loss.item())

    def sync_target(self):
        self.target.load_state_dict(self.online.state_dict())

    def save(self, path):
        torch.save({'model': self.online.state_dict(), 'state_size': self.online.layers[0].in_features,
                    'action_size': self.action_size}, path)

    def load(self, path):
        # weights_only evita desserialização arbitrária via pickle.
        data = torch.load(path, map_location=self.device, weights_only=True)
        if data['state_size'] != self.online.layers[0].in_features or data['action_size'] != self.action_size:
            raise ValueError('Dimensões incompatíveis com o checkpoint')
        self.online.load_state_dict(data['model'])
        self.sync_target()
