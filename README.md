# CDB Finance

O **CDB Finance** é uma aplicação completa para gestão financeira pessoal. Desenvolvida com foco em manutenibilidade e escalabilidade, adota uma arquitetura moderna que separa responsabilidades de forma clara e segue boas práticas de desenvolvimento em Java e JavaScript.

> **Versão atual:** backend `0.9.0` · frontend `0.1.0`

## 🚀 Funcionalidades

O sistema cobre as necessidades de acompanhamento e planejamento financeiro:

- **Dashboard:** Agrega o resultado mensal (receitas, despesas e líquido) por categoria.
- **Contas e Cartões:** Gestão de múltiplas contas bancárias e cartões de crédito, com consulta de saldo por período (mês/ano) e fechamento de competência.
- **Transações:** Registro de créditos, débitos, transferências e parcelas, com filtros, alteração de status e exclusão unitária ou em grupo.
- **Importação de Extratos:** Leitura de faturas e extratos em PDF com fluxo *preview → confirmação*. Detecta tipo de documento e emissor, expande parcelas, sugere categorias e casa lançamentos de cartão. Parsers para **BTG** e **Santander** (cartão e conta).
- **Extratos (Statements):** Histórico mensal por conta, com filtro por status.
- **Categorização:** Classificação de receitas e despesas por Categorias, Centros de Custo e Tags.
- **Perfil e Preferências:** Atualização self-service do próprio usuário (nome, tema, idioma, *locale*, estado da sidebar) via `/api/me`.
- **Atualização em tempo real:** Push de eventos por **SSE**, mantendo a interface sincronizada sem *polling*.
- **API documentada:** Especificação OpenAPI com Swagger UI em `/swagger`.

## 🎯 Público Alvo

O CDB Finance é destinado a **indivíduos e famílias** que desejam um controle rigoroso de suas finanças pessoais. É ideal para pessoas que:
- Precisam consolidar informações de múltiplos bancos e cartões em um só lugar.
- Querem estabelecer e acompanhar orçamentos para não estourar gastos.
- Buscam relatórios detalhados para entender seus hábitos de consumo.
- Preferem ferramentas robustas, com processamento local que garante a privacidade dos dados financeiros, fugindo de planilhas complexas.

## 📐 Escopo e Arquitetura

O projeto abrange frontend e backend, com forte separação arquitetural interna. A arquitetura é **híbrida**: **Vertical Slice** nas features de entrega HTTP (`br.community.feature.*`) sobre **Hexagonal** nos contextos de negócio (`br.community.context.*`). Features se comunicam com os contextos exclusivamente via **Facade**.

### Backend (Java 25 + Quarkus)

- **Vertical Slice Architecture (VSA):** Cada funcionalidade vive isolada em seu pacote, mantendo o código de negócio coeso e independente.
- **Arquitetura Hexagonal:** Dentro de cada contexto de negócio, as camadas são concêntricas:
  - `_0_domain` — modelos, contratos de repositório e portas (sem dependência de framework).
  - `_1_application` — *commands*, *services* e escutadores de eventos (lógica pura).
  - `_2_infrastructure` — implementações concretas de persistência.
  - `UseCase` — orquestração de alto nível, recebe *Commands*.
- **Contextos:** `monetary` (lógica financeira), `security` (usuário e preferências) e `shared` (núcleo comum).
- **Núcleo / Plataforma (`br.community.core`):** Autenticação e autorização (token opaco rotativo, `OwnershipFilter`), observabilidade (log de requisições + correlação), persistência JSON e configuração HTTP transversal.
- **Padrão Result:** Fluxo de negócio sem exceções (`Result` Success/Failure).
- **Persistência:** **JDBC/H2** local (dev: arquivo `./database`; testes: in-memory), com JSON remanescente para `Closing` e o catálogo de centros de custo — processamento e privacidade locais, sem servidor de banco externo.
- **Segurança:** Login com emissão de token, rotas de usuário escopadas por `/api/{uuid}/…` e guarda de propriedade que bloqueia acesso indevido (proteção contra IDOR).
- **Null-Safety & Qualidade:** `NullAway` + `ErrorProne` com anotações JSpecify (`@NullMarked`/`@Nullable`) no ciclo de compilação, falhando o build contra `NullPointerException`.
- **Testes:** Testes unitários com JUnit 5, testes de arquitetura com ArchUnit (ex.: *Resources* não acessam repositórios; *feature* fala com *context* só via *facade*) e testes de integração HTTP com `@QuarkusTest` + RestAssured.

### Frontend (HTML / CSS / JS)

- **Single Page Application (SPA):** Abordagem leve (Vanilla JS/CSS + jQuery 4), sem etapa de build no frontend.
- **Arquitetura em Camadas:** O JavaScript replica o modelo de domínio:
  - `_1_domain` — entidades, validações e regras do cliente.
  - `_2_application` — serviços, orquestração, estado e páginas.
  - `_3_infrastructure` — integrações com a API REST.
- **Design:** Interface moderna (`app.css`) com Dark Mode nativo (aplicado antes do primeiro *paint*, sem *flash*).
- **Request Tracing:** Cada requisição envia o cabeçalho `X-Request-Id` para correlação fim-a-fim no log do backend.

## 🛠️ Tecnologias Utilizadas

| Camada | Stack |
|--------|-------|
| **Linguagens** | Java 25 · JavaScript (ES6+) · HTML5 · CSS3 |
| **Backend** | Quarkus 3.37 (REST, Hibernate Validator, SmallRye OpenAPI/Health, Elytron Security) — modo JVM |
| **Build** | Maven 3.9+ · Quarkus Maven Plugin |
| **Frontend** | Vanilla JS/CSS · jQuery 4 |
| **Utilitários** | Lombok · SLF4J · PDFBox 3.0.5 (leitura de PDF) · Jackson (YAML/JSR-310) · juniversalchardet (detecção de *charset*) |
| **API Docs** | SmallRye OpenAPI (Swagger UI) |
| **Qualidade & Testes** | JUnit 5 · ArchUnit · JaCoCo · NullAway · ErrorProne · JSpecify |

## 🏃 Como Executar

Há dois caminhos: **Maven** (o backend Quarkus serve o frontend estático) ou **Docker Compose** (backend + nginx em containers separados).

### Opção 1 — Maven (desenvolvimento)

```bash
git clone <url-do-repositorio>
cd cdb-judas
mvn quarkus:dev
```

Acesse `http://localhost:8080`. O frontend estático é servido pelo próprio backend.

### Opção 2 — Docker Compose

```bash
docker compose up --build
```

- **Frontend (nginx):** `http://localhost:8081` — serve o estático e faz proxy reverso para a API.
- **Backend (Quarkus):** `http://localhost:8080` — acessível diretamente também.

### Acesso

A aplicação semeia um usuário inicial no primeiro arranque:

| Usuário | Senha |
|---------|-------|
| `admin` | `admin` |

- **Swagger UI:** `/swagger` (ex.: `http://localhost:8080/swagger`).
- **Health:** `/q/health`.

## ⚙️ Configuração

Variáveis de ambiente reconhecidas (ver `docker-compose.yaml`):

| Variável | Descrição | Padrão |
|----------|-----------|--------|
| `STORAGE_JSON_PATH` | Diretório de persistência dos arquivos JSON | `/data` (container) |
| `APP_LOGLEVEL_ROOT` | Nível de log do logger da aplicação (`br.commons`) | `INFO` |

No Docker Compose, o diretório `./data` do host é montado em `/data` no container, persistindo os dados entre execuções.

## 📚 Documentação

A documentação técnica está em [`docs/`](docs/):

- **[Guia Central](CLAUDE.md)** — decomposição funcional + índice de toda a documentação.
- **[Decomposição Funcional](docs/functional_decomposition.md)** — contextos, features e plataforma em detalhe.
- **Backend:** [Arquitetura Hexagonal](docs/backend/hexagonal-architecture.md) · [Padrão Result](docs/backend/result-pattern.md) · [Null-Safety](docs/backend/null-safety.md) · [Lombok](docs/backend/lombok.md) · [Persistência JDBC/H2](docs/backend/persistence-jdbc.md) · [Qualidade & Build](docs/backend/quality-and-build.md)
- **Frontend:** [API Web](docs/frontend/api-web.md)
- **Schema do banco:** diagramas Mermaid em [`docs/`](docs/) (`db-ctx-people`, `db-ctx-monetary`, `db-features`) — fonte da verdade.
