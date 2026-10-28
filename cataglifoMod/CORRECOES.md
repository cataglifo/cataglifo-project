# Correções do diagnóstico do mod

## Problema e resultado

A branch misturava metadados com IDs diferentes e APIs de versões anteriores
ao Minecraft 26.3/Forge 66. O projeto ativo agora usa o namespace `cataglifo`,
registro de eventos compatível com EventBus 7 e um corpo com ações temporárias,
validação de entradas e sensores com formato explícito.

As mudanças foram feitas com assistência de IA e possuem comentários no código.

## Mudanças

- `CorpoMod`: namespace igual ao `mods.toml`, chave de registro de entidade,
  contexto no construtor e barramentos próprios para atributos e comandos.
- `ClientSetup`: anotação do EventBus 7 e registro exclusivo do cliente.
- `CorpoRenderer`: `Identifier` e separação entre entidade e `HumanoidRenderState`.
- `CorpoEntity`: assinatura com `ServerLevel`, chamada `isClientSide()`,
  inicialização da orientação antes da primeira ação e pitch aplicado ao corpo.
- `ControleAcao`: comandos duram cinco ticks por padrão, aceitam duração de
  1 a 20 ticks, limitam translação a [-1, 1] e giro por comando a [-45°, 45°].
  NaN/infinito e durações inválidas são rejeitados antes de alterar a ação.
  O salto é um pulso; a translação expira se nenhuma outra ação chegar.
- `pararAcao`: limpa comandos, navegação, impulso e salto.
- `resetarCorpo`: reset local de posição, vida, fogo, queda e orientação;
  uma entidade morta deve ser substituída, em vez de ressuscitada em remoção.
- APIs de ação, observação e reset exigem a thread do servidor. Uma futura
  thread de rede deverá agendar essas chamadas no servidor.
- Sensores: `schemaVersion=2`, vida, chão, lava, morte, ação ativa e 15 raios.
  Cada raio informa `acertou`, `dist`, `distNormalizada`, `bloco` e `fluido`.
  Ausência de colisão usa alcance de 16 blocos e distância normalizada 1,
  substituindo o antigo -1. Fluido é separado para identificar blocos alagados.
- Logs de observação são condicionais a DEBUG para evitar raycasting apenas
  para mensagens INFO a cada segundo por entidade.
- Build e recursos deixaram de usar o nome `examplemod`. Corrigido o intervalo
  do Minecraft para `[26.3,26.4)` e a descrição dos recursos.
- `CataglifoModExample` foi marcado como protótipo histórico incompleto.
  Nenhum código dessa pasta entra no build independente de `cataglifoMod`.

## Testes automatizados

`ControleAcaoTest` cobre expiração, duração de um tick, salto de um único pulso,
limites de translação e giro, normalização da orientação, parada e rejeição de
NaN/infinito em todos os parâmetros. Também verifica que uma ação inválida
não substitui uma ação válida. A tarefa `controleAcaoTest` integra o `check`.

```bash
./gradlew build
```

Para executar apenas as regressões independentes do jogo, com JDK 25, a partir
de `cataglifoMod/`:

```bash
javac -d build/controle-tests src/main/java/com/cataglifoMod/corpo/ControleAcao.java src/test/java/com/cataglifoMod/corpo/ControleAcaoTest.java
java -cp build/controle-tests com.cataglifoMod.corpo.ControleAcaoTest
```

## Validação realizada nesta alteração

- Compilação direta de todas as classes Java do projeto ativo com JDK 25,
  artefatos oficiais do Minecraft 26.3/Forge 66.0.3 e dependências: passou.
- Compilação com os processadores do `eventbus-validator:7.0.6`: passou.
- Regressões independentes de `ControleAcao`: passaram.
- Metadados TOML/JSON e consistência do namespace: verificados.
- O build Gradle foi tentado, mas o Mavenizer não concluiu o download das
  dependências por timeout de rede no ambiente. Portanto, não há confirmação
  de empacotamento por Gradle nem um JAR de distribuição validado nesta revisão.
- Minecraft não foi aberto. Spawn, renderização, física e raycasting ainda
  precisam de validação no jogo; compilação não comprova esses comportamentos.

## Roteiro no jogo

Use um mundo de testes com comandos habilitados. Os comandos de diagnóstico
exigem permissão de manipulação do mundo. Substitua o seletor por um UUID se
houver várias entidades próximas.

```mcfunction
/summon cataglifo:corpo ~ ~ ~
/cataglifo @e[type=cataglifo:corpo,limit=1,sort=nearest] observar
/cataglifo @e[type=cataglifo:corpo,limit=1,sort=nearest] frente 5
/cataglifo @e[type=cataglifo:corpo,limit=1,sort=nearest] girar 30 0
/cataglifo @e[type=cataglifo:corpo,limit=1,sort=nearest] parar
/cataglifo @e[type=cataglifo:corpo,limit=1,sort=nearest] reset
```

O reset leva a entidade à posição de execução do comando e usa o yaw de quem
executa. Faça o teste em espaço livre. Ele não recria terreno nem gerencia RL.
O movimento `frente` é relativo à orientação do corpo, não a um eixo fixo da grade.

Verificar: aparência, movimento seguido de expiração, giro, parada, reset e
observações em parede, chão, buraco, água, lava e bloco alagado. A detecção de
buracos precisa ser calibrada antes de treinar uma política de navegação.

## Compatibilidade e pendências

O novo ID é `cataglifo:corpo`. IDs antigos não têm migração automática;
use um mundo novo ou uma cópia antes de testar.

O esquema dos sensores mudou: consumidores devem respeitar `schemaVersion=2`.
Os raios continuam limitados a blocos e fluidos, sem detectar outras entidades.

Esta correção prepara o corpo, mas não implementa a conexão TCP/PyTorch,
recompensa, contagem de células visitadas, objetivo ou gerenciador completo de
episódios. Essas regras dependem do contrato de ambiente descrito no plano de
conexão. O futuro gerenciador deverá associar episódio/passo, rejeitar mensagens
antigas, coletar resultado após a ação, recriar entidades mortas e contabilizar
células discretizadas. Uma conexão não deve bloquear a thread do servidor.
