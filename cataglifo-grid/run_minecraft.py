"""Liga um agente ao corpo no Minecraft pela API HTTP do mod.

Com o jogo aberto e o corpo invocado (/summon cataglifo:corpo ~ ~ ~):

    python run_minecraft.py --passos 200
    python run_minecraft.py --passos 200 --log outputs/observacoes.jsonl
    python run_minecraft.py --modelo outputs/minecraft.pt     # DQN treinado com o estado do Minecraft

No single-player o menu de pausa congela o servidor: aperte F3+P no jogo para ele não
pausar ao perder o foco, senão os passos expiram por timeout.
"""
import argparse
import json
from pathlib import Path

from cataglifo.minecraft_client import ApiIndisponivel, CorpoClient, ErroApi, URL_PADRAO
from cataglifo.minecraft_policy import escolher_acao
from cataglifo.minecraft_state import ACOES, TAMANHO_ESTADO, acao_para_passo, codificar_estado


def criar_decisor(modelo):
    if modelo is None:
        return escolher_acao
    from cataglifo.dqn import DQNAgent  # importa torch só se for usar um modelo
    agente = DQNAgent(state_size=TAMANHO_ESTADO, action_size=len(ACOES))
    agente.load(modelo)  # recusa checkpoints com dimensões diferentes (ex.: o do GridWorld)
    return lambda obs: agente.act(codificar_estado(obs), epsilon=0.0)


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('--url', default=URL_PADRAO)
    p.add_argument('--corpo', type=int, default=None, help='id da entidade (padrão: primeiro corpo vivo)')
    p.add_argument('--passos', type=int, default=200)
    p.add_argument('--modelo', type=Path, default=None, help='checkpoint DQN; sem ele usa a política reativa')
    p.add_argument('--reset', action='store_true', help='volta ao ponto inicial antes de começar')
    p.add_argument('--log', type=Path, default=None, help='grava (observação, ação) por passo em JSON Lines')
    p.add_argument('--timeout', type=float, default=15.0)
    args = p.parse_args()
    if args.passos < 1:
        p.error('passos deve ser positivo')

    cliente = CorpoClient(args.url, corpo=args.corpo, timeout=args.timeout)
    decidir = criar_decisor(args.modelo)
    arquivo = None
    if args.log:
        args.log.parent.mkdir(parents=True, exist_ok=True)
        arquivo = args.log.open('a', encoding='utf-8')

    try:
        obs = cliente.reset() if args.reset else cliente.observar()
        for n in range(1, args.passos + 1):
            if obs['morto']:
                print('O corpo morreu; invoque outro com /summon e rode de novo.')
                break
            acao = decidir(obs)
            comando = acao_para_passo(acao)
            if arquivo:
                arquivo.write(json.dumps({'passo': n, 'acao': int(acao), 'obs': obs}, ensure_ascii=False) + '\n')
            obs = cliente.passo(**comando)
            print(f"passo {n:4d} | {ACOES[acao]:<15} | x={obs['x']:.1f} z={obs['z']:.1f} "
                  f"yaw={obs['yaw']:.0f} vida={obs['vida']:.0f} chão={obs['noChao']}")
    except (ApiIndisponivel, ErroApi) as erro:
        print('Erro:', erro)
        return 1
    except KeyboardInterrupt:
        print('\nInterrompido; parando o corpo.')
        try:
            cliente.parar()
        except (ApiIndisponivel, ErroApi):
            pass
    finally:
        if arquivo:
            arquivo.close()
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
