# Configuração do Forge, Gradle e VS Code

Este documento apresenta os passos necessários para configurar o ambiente de desenvolvimento de um projeto **Minecraft Forge 1.21.x**, utilizando **Java 21, Gradle e Visual Studio Code**.

## 1. Pré-requisitos

Antes de abrir o projeto, instale:

- Java JDK 21
- Visual Studio Code
- Gradle for Java #extensão no vscode
- Extension Pack for Java
- Minecraft Java Edition
- Minecraft Forge 1.21.x

## 2. Instalação do Java

O projeto requer **Java 21**.

Após instalar o JDK 21, abra o **terminal integrado do VS Code**:

```text
Terminal → New Terminal
```

ou utilize:

```text
Ctrl + `
```

Verifique a versão instalada:

```bash
java -version
```

O resultado deve indicar a versão **21** do Java.

Exemplo:

```text
openjdk version "21.x.x"
```

Caso outra versão seja apresentada, configure o sistema para utilizar o JDK 21 antes de continuar.

## 3. Configuração do VS Code

Instale as seguintes extensões:

### Extension Pack for Java

Na aba de extensões:

```text
Ctrl + Shift + X
```

Procure por:

```text
Extension Pack for Java
```

e instale a extensão.

### Gradle for Java

Procure também por:

```text
Gradle for Java
```

A extensão utilizada é:

**Gradle for Java — `vscjava.vscode-gradle`**

Ela permite visualizar e executar tarefas do Gradle diretamente pelo VS Code.

[Gradle for Java — Visual Studio Marketplace](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-gradle)

## 4. Configuração do Minecraft Forge

O projeto utiliza **Minecraft Forge 1.21.x**.

### 4.1 Instalar o Minecraft 1.21.x

Primeiramente, abra o **Minecraft Launcher**.

Instale a versão do Minecraft utilizada pelo projeto, por exemplo:

```text
Minecraft 1.21.x
```

Após instalar, **abra o Minecraft nessa versão pelo menos uma vez**.

Isso permite que o launcher faça o download dos arquivos necessários para a versão do Minecraft.

Depois de abrir o jogo, feche-o.

### 4.2 Instalar o Forge

Baixe a versão do **Forge correspondente ao Minecraft 1.21.x** utilizado pelo projeto.

[Download do Minecraft Forge](https://files.minecraftforge.net/)

Execute o instalador do Forge e selecione a opção:

```text
Install client
```

Após a instalação, abra o Minecraft Launcher e verifique se o perfil do Forge foi criado.

Execute o **Minecraft utilizando o Forge 1.21.x pelo menos uma vez**.

Depois que o jogo abrir corretamente, feche-o.

> É importante que a versão do Forge utilizada seja compatível com a versão do Minecraft definida pelo projeto.

## 5. Abrir o projeto

Clone o repositório caso ainda não tenha feito isso:

```bash
git clone <URL_DO_REPOSITORIO>
```

Entre na pasta do projeto:

```bash
cd <NOME_DO_PROJETO>
```

Abra o projeto no VS Code:

```bash
code .
```

Também é possível abrir pelo próprio VS Code:

```text
File → Open Folder
```

Selecione a **pasta raiz do projeto**.

A estrutura deverá ser semelhante a:

```text
projeto/
├── gradle/
├── src/
├── build.gradle
├── gradlew
├── gradlew.bat
├── settings.gradle
└── ...
```

A estrutura dos arquivos estará um pouco diferente pelos arquivos do forge


## 7. Configuração do ambiente Forge

Depois que o projeto estiver aberto no VS Code, execute:

```bash
./gradlew build
```

Isso fará a compilação do projeto e verificará se as dependências e configurações do Forge estão funcionando corretamente.

Para executar uma instância de desenvolvimento do Minecraft, utilize:

```bash
./gradlew runClient
```

Essa tarefa inicia uma instância de desenvolvimento do Minecraft configurada pelo projeto.

> As tarefas disponíveis podem variar de acordo com a versão do Forge e com a configuração do projeto.

## 8. Referências

- [Gradle for Java — Visual Studio Marketplace](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-gradle)
- [Minecraft Forge](https://files.minecraftforge.net/)
- [Documentação oficial do Forge](https://docs.minecraftforge.net/en/1.21.x/)
- [Documentação oficial do Gradle](https://docs.gradle.org/)

Os links acima devem ser utilizados como **fontes de estudo e consulta** para compreender a configuração e o desenvolvimento de mods utilizando Forge e Gradle.
