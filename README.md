# Cataglifo — mod Minecraft

Nesta branch (`modMine`), o projeto ativo é **`cataglifoMod/`**.
`CataglifoModExample/` é um protótipo histórico incompleto; não use essa pasta
para compilar ou executar o mod atual. Ela é preservada para consulta.

## Compilar

Use JDK 25, Minecraft 26.3 e Forge 66.0.3, conforme o build versionado.
O wrapper fixa Gradle 9.7.1. O primeiro build precisa de internet para baixar
Gradle, a toolchain e as dependências.

```bash
cd cataglifoMod
./gradlew build
./gradlew runClient
```

No Windows, use `gradlew.bat build` e `gradlew.bat runClient`.

No mundo de testes com comandos habilitados:

```mcfunction
/summon cataglifo:corpo ~ ~ ~
```

O identificador anterior `corpoia:corpo` foi substituído. Use um mundo de testes
novo ou uma cópia: entidades salvas com IDs antigos não são migradas automaticamente.

## Correções e validação

Consulte [o registro técnico](cataglifoMod/CORRECOES.md) para as mudanças,
limites da validação e próximos passos. Os comentários no código explicam
a migração das APIs e as regras de controle.
