# Cataglifo

> **README provisório — documento sujeito a alterações conforme o projeto evolui.**  
> Este arquivo representa apenas a visão inicial do Cataglifo e será refinado ao longo das próximas sprints, conforme decisões técnicas, experimentos e mudanças de escopo forem consolidados.

---

## Visão geral

O **Cataglifo** é um projeto experimental de Inteligência Artificial voltado ao desenvolvimento de um agente autônomo capaz de aprender a interagir com ambientes virtuais.

O projeto busca explorar conceitos de **Machine Learning**, **Deep Learning** e **aprendizado por interação**, com foco inicial na construção de uma base técnica capaz de permitir que o agente perceba informações do ambiente, tome decisões, execute ações e tenha seu desempenho avaliado por métricas previamente definidas.

Neste primeiro momento, o objetivo não é construir imediatamente um agente complexo, mas desenvolver o projeto de forma incremental, validando cada etapa antes de aumentar a complexidade do ambiente e do comportamento esperado.

---

## Origem do nome

O nome **Cataglifo** foi inspirado nas **formigas-do-deserto do gênero _Cataglyphis_**.

Essas formigas são conhecidas por sua capacidade de navegação em ambientes extremos, conseguindo se afastar do ninho em busca de alimento e retornar mesmo em regiões com poucas referências visuais.

A inspiração combina com a proposta do projeto: desenvolver um agente que seja capaz de **perceber o ambiente, tomar decisões e aprender estratégias de interação e navegação de forma progressivamente autônoma**.

O nome não implica que o comportamento biológico das formigas será necessariamente reproduzido pelo agente; trata-se principalmente de uma inspiração conceitual para a identidade do projeto.

---

## Objetivo

Desenvolver e avaliar um agente de Inteligência Artificial capaz de aprender comportamentos em um ambiente virtual por meio de interação, experimentação e análise de resultados.

Durante o desenvolvimento, serão investigadas diferentes formas de representar o ambiente, estruturar as ações possíveis, definir recompensas e avaliar o desempenho do agente.

---

## Objetivos iniciais

Nesta fase inicial, o projeto pretende:

- estudar os fundamentos necessários de Machine Learning;
- definir o problema de aprendizado do agente;
- estabelecer métricas e critérios de sucesso;
- definir quais informações serão fornecidas ao agente;
- definir quais ações estarão disponíveis;
- escolher um método inicial de aprendizado;
- desenvolver uma primeira rede neural;
- construir ou configurar um ambiente virtual de testes;
- integrar o agente ao ambiente;
- executar ciclos de treinamento;
- registrar e analisar os resultados obtidos.

---

## Estado atual

> **Fase atual: Fundação técnica / definição inicial**

O projeto ainda se encontra em fase de estruturação.

Até o momento, foram definidas algumas ferramentas iniciais de desenvolvimento, enquanto decisões relacionadas ao método de aprendizado, arquitetura final do agente, ambiente de interação e métricas ainda estão em estudo.

Por esse motivo, **este README não deve ser interpretado como documentação definitiva do projeto**.

---

## Stack inicial

| Área | Tecnologia |
|---|---|
| Ambiente de desenvolvimento | Visual Studio Code |
| Linguagem | Python |
| Análise e manipulação de dados | pandas |
| Machine Learning | scikit-learn |
| Redes neurais / Deep Learning | PyTorch |
| Versionamento | Git + GitHub |
| Gestão de tarefas | GitHub Projects / Issues |
| Registro de atividades | Google Docs |

A stack poderá ser alterada conforme os experimentos e necessidades técnicas do projeto.

---

## Estrutura conceitual inicial

O agente será desenvolvido inicialmente em torno de um ciclo semelhante a:

```text
Ambiente
   ↓
Estado observado
   ↓
Agente
   ↓
Decisão
   ↓
Ação
   ↓
Resultado
   ↓
Avaliação / Recompensa
   ↓
Novo estado
```

Ainda serão definidos durante as próximas sprints:

- representação do estado;
- espaço de ações;
- função de recompensa;
- arquitetura da rede neural;
- método de aprendizado;
- métricas de avaliação;
- ambiente virtual definitivo.

---

## Metodologia de desenvolvimento

O desenvolvimento do Cataglifo está sendo organizado de forma incremental, utilizando práticas de gestão ágil.

O trabalho será dividido em ciclos curtos de desenvolvimento, estudo, experimentação e revisão.

De forma geral, o fluxo será:

```text
Planejamento
    ↓
Estudo
    ↓
Implementação
    ↓
Experimento
    ↓
Avaliação
    ↓
Replanejamento
```

O escopo poderá ser revisado ao longo do projeto com base nos resultados dos experimentos e nas limitações encontradas.

---

## Organização do projeto

A gestão das atividades utiliza recursos do GitHub, principalmente:

- **Issues** para tarefas;
- **GitHub Projects** para acompanhamento do fluxo de trabalho;
- **Roadmap** para planejamento temporal;
- **Git** para controle de versão;
- **Pull Requests** para integração e revisão de alterações quando aplicável.

Documentações complementares, relatórios de estudo, decisões técnicas e registros de sprint poderão ser mantidos separadamente da base principal de código.

---

## Critério de sucesso

Os critérios definitivos ainda serão definidos.

A avaliação do projeto deverá utilizar métricas objetivas, evitando considerar apenas percepções subjetivas sobre o comportamento do agente.

Exemplos de aspectos que poderão ser avaliados:

- taxa de sucesso em determinada tarefa;
- tempo necessário para atingir um objetivo;
- evolução do desempenho durante o treinamento;
- quantidade de recompensas acumuladas;
- capacidade de adaptação a situações não vistas anteriormente;
- consistência dos resultados entre diferentes execuções.

> **Essas métricas são apenas possibilidades e ainda não representam os critérios finais do projeto.**

---

## Documentação

A documentação será atualizada progressivamente durante o desenvolvimento.

Entre os documentos previstos estão:

```text
docs/
├── sprints/
├── studies/
├── architecture/
├── experiments/
└── retrospectives/
```

A organização definitiva poderá mudar conforme o projeto amadurecer.

---

## Equipe

Projeto desenvolvido como parte de um trabalho acadêmico de Ciência da Computação.

| Nome | Função |
|---|---|
| João Gabriel Guedes Vianna | Gestor de Configuração e Integração |
| Pedro Huck Henrique | Idealização e Desenvolvimento |
| Tulio Gonçalves Vieira | Documentação e Desenvolvimento |

---

## Aviso sobre este README

Este README é **intencionalmente provisório**.

O Cataglifo ainda está em uma etapa inicial de pesquisa e definição, portanto diversas decisões descritas aqui poderão mudar.

Este documento será atualizado conforme forem consolidados:

- o objetivo experimental definitivo;
- o ambiente utilizado;
- o método de aprendizado;
- a arquitetura do agente;
- as métricas;
- os resultados dos primeiros experimentos;
- o escopo final do projeto.

A intenção atual é registrar de forma clara **onde o projeto está agora**, e não antecipar decisões que ainda não foram tomadas.
