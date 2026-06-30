# CDB Finance — Guia Central

Gestor de finanças pessoais. Backend **Java 25 + Spring Boot 4**; frontend **SPA Vanilla JS/CSS + jQuery 4** (servido pelo próprio backend). Arquitetura híbrida: **Vertical Slice** nas features de entrega HTTP (`br.community.feature.*`) sobre **Hexagonal** nos contextos de negócio (`br.community.context.*`) — features falam com contextos **apenas via Facade**. Persistência: **JDBC/H2** (dev: file `./database`; teste: in-memory) + JSON para `Closing` e o catálogo de centros de custo. Rotas de dados escopadas por `/api/{uuid}/…` com guarda de propriedade (anti-IDOR).

> Este é o índice central. As diretrizes detalhadas vivem nos `CLAUDE.md` aninhados (`src/main/java/`, `web/`) e em `docs/`.
> **Schema do banco: os diagramas `docs/*.mermaid` são a fonte da verdade** — o código (DDL em `Database`) conforma ao diagrama, nunca o inverso.

---

## Decomposição Funcional

### Contextos de negócio — `br.community.context.*` (Hexagonal, livre de Spring; DI via `Registry`)
- **`monetary`** — lógica financeira. Facade `MonetaryContext`; modelos `MonetaryAccount`, `MonetaryTransaction`, `MonetaryCenter`, `MonthlyBalance`, `MonetaryStatement`; use cases `AccountUseCase`, `TransactionUseCase`, `MetadataUseCase`.
- **`people`** — identidade, usuário e preferências. Agregado `User` (+ `Preferences`: theme/language/locale/sidebarCollapsed).
- **`shared`** — núcleo comum dos contextos (`DomainEvent`, `DomainError`, `SharedModule`).

### Features de usuário — `br.community.feature.user.*` (HTTP, `/api/{uuid}/…`)
- **`dashboard`** — agrega resultado mensal por categoria (receitas/despesas/líquido).
- **`accounts`** — CRUD de contas e cartões, com sub-áreas:
  - **saldo** — consulta por período (`?period=yyyyMM` / `?year=yyyy`);
  - **closing** — período de fechamento (a validação de data é **fronteira da feature**, não do contexto);
  - **statement** — histórico mensal por conta;
  - **importação** — leitura de PDF (preview→confirm), parsers **BTG** e **Santander** (cartão + conta), expansão de parcelas, sugestão de categoria, casamento de cartão;
  - **transactions** — créditos, débitos, transferências, parcelas; filtros, patch de status, delete unitário/em grupo.
- **`categories`** / **`tags`** — classificação macro/micro; mudanças propagadas via **SSE**.
- **`profile`** — self-service `/api/me` (nome + preferências write-through).
- **`stream`** — canal SSE (`/api/{uuid}/stream`).

### Features de sistema — `br.community.feature.system.*` (sem `{uuid}`)
- **`auth`** — login e emissão de token (`POST /login`, JWT rotativo).
- **`costcenter`** — catálogo somente-leitura (`GET /api/cost-center`).

### Plataforma — `br.community.core`
Autenticação/autorização (JWT, `OwnershipInterceptor`), observabilidade (log de requisição + MDC), persistência JSON, config HTTP/OpenAPI, `ContextBridge` (costura Spring↔`Registry`).

### Framework comum — `br.commons`
`Result` (Success/Failure), `MessageBus`, Logger próprio (console + arquivo rotativo), abstrações de persistência (JSON + pool JDBC), leitor YAML, `PdfBoxTextExtractor`, utilidades multiplataforma.

---

## Índice de Referências

| Documento | Conteúdo |
|---|---|
| `README.md` | Visão geral, como executar (Maven/Docker), stack, configuração |
| `docs/functional_decomposition.md` | Decomposição por feature, em detalhe¹ |
| **`src/main/java/CLAUDE.md`** | **Diretrizes backend** — índice operacional: VSA/Hexagonal, Result, Lombok, Null-Safety, Testes/ArchUnit, Qualidade & Build |
| **`web/CLAUDE.md`** | **Diretrizes frontend** — estilo + padrões 001–006 (request tracing, token rotativo, namespace de usuário, preferências server-owned) |
| `docs/backend/hexagonal-architecture.md` | Camadas Resource → UseCase → Service → Repository |
| `docs/backend/result-pattern.md` | Result / Railway + desembrulho `.get()`/fatal nos adapters |
| `docs/backend/null-safety.md` | JSpecify `@NullMarked` + NullAway/ErrorProne |
| `docs/backend/lombok.md` | `val` e suas exceções |
| `docs/backend/persistence-jdbc.md` | JDBC/H2, schema = mermaid, ciclo de vida do DB de dev, colunas anuláveis |
| `docs/backend/quality-and-build.md` | Gate PMD/CPD, gotchas de build Java 25, verificação sem `mvn`, setup de IDE |
| `docs/frontend/api-web.md` | Integração do frontend com a API REST |
| `docs/architecture.mermaid` | Diagrama de arquitetura geral |
| `docs/db-ctx-people.mermaid` · `docs/db-ctx-monetary.mermaid` · `docs/db-features.mermaid` | **Schema ER canônico** (fonte da verdade do banco) |
| `docs/extrato/` | PDFs de exemplo (BTG, Santander) para testar a importação |

¹ Predata a migração JSON→JDBC e a realocação de Category/Tag para features de usuário; onde divergir do código/mermaid, **o código/mermaid vence**.

---

## Onde está o quê

- **Convenções de código (backend)** → `docs/backend/*` (índice em `src/main/java/CLAUDE.md`).
- **Arquitetura e regras (frontend)** → `web/CLAUDE.md`.
- **Schema do banco** → `docs/*.mermaid` (canônico; `Database` conforma).
- **Gotchas de build / IDE / qualidade** → `docs/backend/quality-and-build.md`.
