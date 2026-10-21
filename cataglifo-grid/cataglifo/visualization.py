"""Renderiza episódios do GridWorld sem alterar a lógica do ambiente."""

from dataclasses import dataclass
import os
import select
import sys
import time

from .environment import ACTION_NAMES


ACTION_ARROWS = ("↑", "↓", "←", "→")


@dataclass
class FrameData:
    episode: int
    step: int
    max_steps: int
    reward: float
    action: int = None
    epsilon: float = None
    collisions: int = 0
    status: str = "EM EXECUÇÃO"
    blocked: bool = False


def map_lines(env):
    """Retorna o mapa atual como texto, útil também para testes e logs."""
    result = []
    for row_index, row in enumerate(env.grid):
        rendered = []
        for column_index, original in enumerate(row):
            pos = (row_index, column_index)
            if pos == env.pos:
                char = "A"
            elif pos == env.start:
                char = "S"
            elif pos == env.goal:
                char = "G"
            elif original == "#":
                char = "#"
            elif pos in env.visited:
                char = "*"
            else:
                char = "."
            rendered.append(char)
        result.append("".join(rendered))
    return result


def render_text(env, data):
    """Produz uma representação sem códigos ANSI (fallback e testes)."""
    if data.action is None:
        action = "—"
    else:
        action = f"{ACTION_NAMES[data.action].upper()} {ACTION_ARROWS[data.action]}"
    epsilon = "—" if data.epsilon is None else f"{data.epsilon:.3f}"
    collision = " (COLISÃO)" if data.blocked else ""
    metrics = [
        "CATAGLIFO — Visualização do agente",
        f"Episódio: {data.episode}",
        f"Passo: {data.step}/{data.max_steps}",
        f"Recompensa acumulada: {data.reward:+.2f}",
        f"Exploração (epsilon): {epsilon}",
        *map_lines(env),
        f"Última ação: {action}{collision}",
        f"Posição: {env.pos}",
        f"Posições únicas: {len(env.visited)}",
        f"Colisões: {data.collisions}",
        f"Status: {data.status}",
    ]
    return "\n".join(metrics)


class KeyboardControls:
    """Leitura não bloqueante de teclas, somente em terminais interativos."""

    def __init__(self):
        self.enabled = bool(getattr(sys.stdin, "isatty", lambda: False)())
        self._old_settings = None

    def __enter__(self):
        if self.enabled and os.name != "nt":
            try:
                import termios
                import tty
                self._old_settings = termios.tcgetattr(sys.stdin)
                tty.setcbreak(sys.stdin.fileno())
            # termios.error deriva de OSError nas versões suportadas.
            except (ImportError, OSError):
                self.enabled = False
        return self

    def __exit__(self, *_):
        if self._old_settings is not None:
            import termios
            termios.tcsetattr(sys.stdin, termios.TCSADRAIN, self._old_settings)

    def read(self):
        if not self.enabled:
            return None
        if os.name == "nt":
            import msvcrt
            return msvcrt.getwch().lower() if msvcrt.kbhit() else None
        readable, _, _ = select.select([sys.stdin], [], [], 0)
        return sys.stdin.read(1).lower() if readable else None


class VisualizationStopped(Exception):
    """Encerramento solicitado pelo usuário; não representa erro."""


class TerminalVisualizer:
    def __init__(self, speed=0.3, enabled=True):
        if speed < 0:
            raise ValueError("speed não pode ser negativo")
        self.speed = speed
        self.enabled = enabled
        self.paused = False
        self.controls = KeyboardControls()
        self._live = None

    def __enter__(self):
        if not self.enabled:
            return self
        try:
            from rich.console import Console
            from rich.live import Live
            self._live = Live("", console=Console(), refresh_per_second=12)
            self._live.__enter__()
        except ImportError:
            self._live = None
        self.controls.__enter__()
        return self

    def __exit__(self, *exc):
        self.controls.__exit__(*exc)
        if self._live is not None:
            self._live.__exit__(*exc)

    def update(self, env, data, wait=True):
        if not self.enabled:
            return
        output = render_text(env, data)
        if self._live is not None:
            from rich.panel import Panel
            self._live.update(Panel(output, title="Cataglifo", border_style="cyan"), refresh=True)
        else:
            print("\n" + output, flush=True)
        if wait:
            self._wait_for_next_frame()

    def _wait_for_next_frame(self):
        deadline = time.monotonic() + self.speed
        while self.paused or time.monotonic() < deadline:
            key = self.controls.read()
            if key == "q":
                raise VisualizationStopped()
            if key in (" ", "p"):
                self.paused = not self.paused
                if not self.paused:
                    deadline = time.monotonic() + self.speed
            elif key in ("n", "s") and self.paused:
                return
            time.sleep(0.02)
