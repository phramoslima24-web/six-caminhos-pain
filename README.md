# Rinnegan: Six Paths (addon para Narutoto 0.4.1, Forge 1.20.1)

## Como compilar pelo GitHub
1. Crie uma Release no mesmo repositório com a tag `narutoto-base`.
2. Anexe nessa Release o arquivo JAR original do Narutoto 0.4.1 Forge (pode manter o nome original).
3. Envie os arquivos deste projeto para `main` ou `master`.
4. Abra **Actions > Build Rinnegan Six Paths**. O workflow baixa a dependência da Release e compila o addon.
5. Baixe o artefato `rinnegan-six-paths` da execução concluída; o mod compilado estará dentro dele.

Para compilar localmente, coloque `narutoto-0_4_1-forge.jar` em `libs/` e execute `gradle build` com JDK 17 e Gradle 8.1.1.

## Controles (rebindáveis)
- `N` alterna o Caminho selecionado.
- `B` usa o poder do Caminho selecionado.
- `M` invoca ou dispensa os seis corpos de Pain.
- Agachar + `B` com Tendō usa Banshō Ten'in; sem agachar usa Shinra Tensei.
- Agachar + `B` com Jigokudō restaura aliados.
Confira conflitos em Opções > Controles.

## Yarico e os Seis Caminhos
O jogador é o controlador principal (Yarico). A tecla `M` invoca seis corpos aliados nomeados Tendō, Shuradō, Ningendō, Chikushōdō, Gakidō e Jigokudō. Eles seguem o controlador, atacam o alvo que ele estiver mirando e podem ser dispensados com `M` novamente. A invocação custa 1200 de chakra e dura até 10 minutos.

Os corpos reutilizam as entidades GeckoLib, os modelos e as texturas de Pain/Jigokudō que já vêm no JAR original do Narutoto. Como a versão 0.4.1 fornece o modelo de Pain e o de Jigokudō (não seis modelos individuais), cinco corpos usam o visual Pain e o sexto usa o visual Jigokudō, cada um com seu nome de Caminho. Isso mantém o visual nativo do mod sem inventar modelos genéricos.

## Habilidades (exigem Rinnegan ativo, usa o chakra do mod base)
| Caminho | Efeito | Chakra | Recarga |
|---|---|---|---|
| Tendō | sem agachar: empurra inimigos próximos (Shinra Tensei); agachado: puxa o alvo (Banshō Ten'in) | 450 | 12 s |
| Shuradō | rajada mecânica de chakra no alvo | 500 | 15 s |
| Ningendo | alvo na mira: 24 de dano mágico, fraqueza/lentidão, drena até 400 de chakra (jogador) e devolve metade | 300 | 20 s |
| Chikushodo | 2 lobos + 1 golem aliados por 45 s, já atacam seu alvo | 400 | 45 s |
| Gakido | liga/desliga: cancela dano e converte em chakra, absorve projéteis a 3 blocos, drena 40 de quem te bate; custo contínuo | 4/5 ticks | 2 s |
| Jigokudo | mira: revela vida/chakra, brilho + lentidão. Agachado: cura aliados em 12 blocos, remove efeitos negativos, +300 chakra | 500 | 30 s |
| Hastes Negras | prende até 3 alvos por 8 s (imobiliza, drena 40 chakra/0,5 s de jogadores) | 300 | 25 s |
| Rinne Tensei | revive jogadores (online, já renasceram) e NPCs do mod mortos nos últimos 10 min a 64 blocos; devolve itens do chão; você fica debilitado 3 min | 3000 | 10 min |

Valores ajustáveis nas constantes no topo de `Abilities.java` e `PathEvents.java`.


## Correção de dependência (v0.2.1)

O JAR base do Narutoto é carregado como arquivo local com `fg.deobf(files(...))`.
Isso evita que o Gradle tente resolver uma dependência flatDir sem versão
(`null_mapped_official_1.20.1`).

No GitHub Actions, a Release `narutoto-base` precisa conter o JAR original.
O workflow baixa o asset e o salva como `libs/narutoto-0_4_1-forge.jar`.


## v0.2.2 — Proteção automática dos Seis Caminhos

- Quando qualquer Mob causar dano ao jogador que invocou os Seis Caminhos,
  o dano é interceptado enquanto pelo menos um dos corpos estiver vivo e a até
  64 blocos.
- Todos os corpos invocados recebem o agressor como alvo e avançam para atacá-lo.
- A regra também reconhece ataques de projéteis quando o dono do projétil é um Mob.
- O sistema não cancela ataques de outros jogadores.

### Skins recebidas
As quatro skins enviadas foram adicionadas em
`assets/rinnegan_paths/textures/entity/paths/`, junto com a imagem de referência.
Elas estão preservadas como assets do projeto; ainda precisam ser ligadas aos
renderers/modelos GeckoLib correspondentes para aparecerem nos corpos em jogo.
Faltam as skins de Tendō e Chikushōdō para completar o conjunto.
