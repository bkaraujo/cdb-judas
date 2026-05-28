# CDB Finance

O **CDB Finance** é uma aplicação completa para gestão financeira pessoal. Desenvolvida com foco em manutenibilidade e escalabilidade, a aplicação utiliza uma arquitetura moderna dividindo as responsabilidades de forma clara e seguindo as melhores práticas de desenvolvimento em Java e JavaScript.

## 🚀 Funcionalidades

O sistema foi projetado para cobrir todas as necessidades de acompanhamento e planejamento financeiro:

- **Dashboard:** Visão geral e agregações de receitas, despesas e saldos.
- **Contas e Cartões:** Gestão de múltiplas contas bancárias e cartões de crédito.
- **Transações:** Registro, acompanhamento e importação de transações diárias a partir de arquivos (como faturas em PDF/TXT).
- **Categorização:** Classificação de despesas e receitas usando Categorias, Centros de Custo e Tags.
- **Orçamento (Budget):** Definição de limites e planejamento financeiro por períodos.
- **Faturas e Extratos (Statements):** Acompanhamento de faturas de cartão de crédito e extratos detalhados.
- **Contas a Pagar (Payables):** Controle de obrigações financeiras e vencimentos.
- **Relatórios:** Geração de relatórios para análises profundas do comportamento financeiro.

## 🎯 Público Alvo

O CDB Finance é destinado a **indivíduos e famílias** que desejam um controle rigoroso de suas finanças pessoais. É ideal para pessoas que:
- Precisam consolidar informações de múltiplos bancos e cartões de crédito em um só lugar.
- Querem estabelecer e acompanhar orçamentos mensais para não estourar gastos.
- Buscam relatórios detalhados para entender seus hábitos de consumo.
- Preferem ferramentas robustas, que possuam processamento local e garantam a privacidade de seus dados financeiros, fugindo de planilhas complexas.

## 📐 Escopo e Arquitetura

O projeto abrange tanto o frontend quanto o backend, servidos de maneira integrada, com uma forte separação arquitetural interna.

### Backend (Java 25 + Spring Boot 4)
- **Vertical Slice Architecture (VSA):** Funcionalidades isoladas em pacotes (ex: `feature/creditcards`, `feature/orcamento`), garantindo que o código de negócio de cada módulo seja coeso e independente. A comunicação entre módulos ocorre através de *facades* do contexto.
- **Arquitetura Hexagonal:** Separação estrita de responsabilidades dentro de cada vertical:
  - `Resource` (Interface Web via REST / Tradução HTTP)
  - `UseCase` (Orquestração de lógica e serviços, recebe Commands)
  - `Service` (Regras de negócio e encapsulamento de repositórios)
  - `Repository` (Persistência)
- **Null-Safety & Qualidade:** Uso estrito de `NullAway` e `ErrorProne` no ciclo de compilação para garantir um código limpo e seguro contra NullPointerExceptions.
- **Testes:** Ampla cobertura de testes unitários com JUnit 5 e testes automatizados de arquitetura através do ArchUnit.

### Frontend (HTML / CSS / JS)
- **Single Page Application (SPA):** Construído com uma abordagem leve (Vanilla JS/CSS e jQuery 4), sem complexidade de frameworks de build no frontend. Todo o estático é servido pelo backend.
- **Arquitetura em Camadas:** O JavaScript é estruturado replicando um modelo de domínio:
  - `_1_domain`: Entidades e regras do cliente.
  - `_2_application`: Serviços, orquestração e estado.
  - `_3_infrastructure`: Integrações com a API REST.
- **Design:** Interface moderna (`app.css`) com suporte a Dark Mode nativo.

## 🛠️ Tecnologias Utilizadas

- **Linguagem:** Java 25 (Preview Features) / JavaScript / HTML5 / CSS3
- **Framework Backend:** Spring Boot 4.0.2
- **Build Tool:** Maven
- **Utilitários:** Lombok, SLF4J, PDFBox (leitura de documentos), Jackson.
- **Qualidade e Testes:** JUnit 5, ArchUnit, JaCoCo, NullAway, ErrorProne.

## 🏃 Como Executar

1. Clone o repositório:
   ```bash
   git clone <url-do-repositorio>
   cd cdb-judas
   ```
2. Compile e execute utilizando o Maven:
   ```bash
   mvn spring-boot:run
   ```
3. Acesse no navegador:
   Abra `http://localhost:8080` para utilizar a aplicação.
