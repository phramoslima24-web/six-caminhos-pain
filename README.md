# Rinnegan: Six Paths

Addon para **Narutoto: Ninja World 0.4.1**, Minecraft **1.20.1** e **Minecraft Forge 47.3.0**.

## Preparar o repositório no GitHub

1. Crie um repositório novo no GitHub.
2. Envie todos os arquivos e pastas deste projeto, mantendo a estrutura.
3. Antes de compilar, coloque o arquivo original `narutoto-0_4_1-forge.jar` dentro da pasta `libs/`.
4. Envie também esse JAR ao repositório, no caminho exato `libs/narutoto-0_4_1-forge.jar`.
5. Faça commit na branch `main` (ou `master`).
6. Abra a aba **Actions** e selecione **Compilar Rinnegan Six Paths**.

> **Dependência obrigatória:** o código do addon referencia classes internas do Narutoto. Por isso, o JAR original precisa estar presente em `libs/` durante a compilação. O arquivo `.txt` que acompanha a pasta `libs` é apenas um aviso e não substitui o JAR.

## Compilar localmente

- Instale o JDK 17.
- Coloque `narutoto-0_4_1-forge.jar` em `libs/`.
- Instale Gradle 8.1.1 e execute na pasta do projeto:

```bash
gradle --no-daemon build
```

O resultado será gerado em `build/libs/`.

## Baixar pelo GitHub Actions

Quando o workflow terminar com sucesso:

1. Abra a execução concluída na aba **Actions**.
2. Vá até **Artifacts**.
3. Baixe `rinnegan-six-paths-jar`.

## Controles

- `N`: alterna o Caminho.
- `B`: usa a habilidade selecionada.
- Os controles podem ser alterados em **Opções > Controles**.

## Habilidades incluídas nesta versão

| Caminho / técnica | Efeito | Chakra | Recarga |
|---|---|---:|---:|
| Ningendo | Alvo na mira: dano mágico, fraqueza/lentidão e drenagem de chakra de jogadores | 300 | 20 s |
| Chikushodo | Invoca dois lobos e um golem aliados por 45 segundos | 400 | 45 s |
| Gakido | Barreira que absorve dano/projéteis e converte parte em chakra | 4 por 5 ticks | 2 s |
| Jigokudo | Interroga alvo ou cura aliados próximos | 500 | 30 s |
| Hastes Negras | Prende até três alvos e drena chakra de jogadores | 300 | 25 s |
| Rinne Tensei | Tenta reviver jogadores/NPCs elegíveis registrados pelo addon | 3000 | 10 min |

Os valores podem ser ajustados nas constantes de `Abilities.java` e `PathEvents.java`.

## Observações

- Este pacote foi preparado para envio ao GitHub e compilação automatizada.
- A compilação ainda precisa ser executada no GitHub Actions para confirmar compatibilidade real.
- Esta versão não adiciona ainda Shinra Tensei, Bansho Ten'in ou as habilidades do Shurado.
- Para executar o jogo em ambiente de desenvolvimento, podem ser necessárias dependências adicionais do mod base, como GeckoLib, Photon, Pehkui e Player Animator.
