# CDB Finance — Guia Central

Gestor de finanças pessoais. Backend **Java 25 + Quarkus** (JVM mode); frontend **SPA Vanilla JS/CSS + jQuery 4** (servido pelo próprio backend). Arquitetura híbrida: **Vertical Slice** nas features de entrega HTTP (`br.cdb.feature.*`) sobre **Hexagonal** nos contextos de negócio (`br.cdb.context.*`) — features falam com contextos **apenas via os use cases expostos pela Facade** (`MonetaryContext.uc*()`); os `*Resource` que atuam sobre dados do usuário delegam a orquestração ao **`UserUseCase`** (`br.cdb.feature.user`, único use case da fatia) e fazem só tradução de formato. Persistência: **JDBC/H2** (dev: file `./database`; teste: in-memory) + JSON para `Closing` e o catálogo de centros de custo. Rotas de dados escopadas por `/api/{uuid}/…` com guarda de propriedade (anti-IDOR).

> Este é o índice central. As diretrizes detalhadas vivem nos `CLAUDE.md` de cada módulo (`br-parent/`, `br-commons/src/main/java/`, `br-context-people/src/main/java/`, `br-context-monetary/src/main/java/`, `br-application/src/main/java/`, `web/`) e em `docs/`.
> **Schema do banco: os diagramas `docs/*.mermaid` são a fonte da verdade** — o código (DDL em `Database`) conforma ao diagrama, nunca o inverso.

---

## Decomposição Funcional

### Módulos Maven (empacotamento físico)
`br-parent` (pom pai — versões, plugins, gate de qualidade; ver `br-parent/CLAUDE.md`) · `br-commons` (framework comum, sem `br.cdb.*`) · `br-context-people` + `br-context-monetary` (contextos hexagonais, dependem só de `br-commons`) · `br-application` (borda HTTP/CDI — `feature`/`core`/`infra` — depende dos três anteriores). `web/` (frontend) não é módulo Maven. Cada um tem seu próprio `CLAUDE.md` operacional.

### Contextos de negócio — `br.cdb.context.*` (Hexagonal, livre de framework; DI via `Registry`)
- **`monetary`** (módulo `br-context-monetary`) — lógica financeira. Facade `MonetaryUseCases` (acessores estáticos para os use cases); modelos `Account`, `Balance`, `CostCenter`, `CreditCard`, `Statement`, `Transaction`; use cases `AccountUseCase`, `CreditCardUseCase`, `CostCenterUseCase`, `TransactionUseCase`.
- **`people`** (módulo `br-context-people`) — identidade mínima. Facade `PeopleContext`; modelo `Person` (id/name/locale/language). **Não** inclui `User` (login) nem `Preferences` — esses são agregados de `br-application` (`br.cdb.core.web.security.User` e `br.cdb.feature.user.profile.preference.Preferences`), não deste contexto. Hoje `PeopleContext` está montado mas não é consumido por nenhuma feature — ver `br-context-people/src/main/java/CLAUDE.md`.
- **Núcleo comum entre contextos**: não existe um contexto `shared` — o pacote `context..shared..` é apenas reservado nas regras ArchUnit, sem implementação. O vocabulário compartilhado hoje (erro/evento de domínio) é `br.commons.business.{BusinessError,BusinessEvent,BusinessException}`, em `br-commons`.

### Features de entrega — `br.cdb.feature.*` (achatadas direto sob `feature.*`, sem prefixo `user.`/`system.`)
- **`dashboard`** — agrega resultado mensal por categoria (receitas/despesas/líquido). `/api/{uuid}/…`.
- **`finance.accounts`** — CRUD de contas e cartões, com sub-áreas (`/api/{uuid}/…`):
  - **saldo** — consulta por período (`?period=yyyyMM` / `?year=yyyy`);
  - **closing** — período de fechamento (a validação de data é **fronteira da feature**, não do contexto);
  - **statement** — histórico mensal por conta;
  - **importação** — leitura de PDF (preview→confirm), parsers **BTG** e **Santander** (cartão + conta), expansão de parcelas, sugestão de categoria, casamento de cartão;
  - **transactions** — créditos, débitos, transferências, parcelas; filtros, patch de status, delete unitário/em grupo.
- **`finance.categories`** / **`finance.tags`** — classificação macro/micro; mudanças propagadas via **SSE**. `/api/{uuid}/…`.
- **`finance.costcenter`** — catálogo somente-leitura (`GET /api/cost-center`, sem `{uuid}`).
- **`finance.deletion`** — vocabulário de exclusão compartilhado entre as fatias de `finance.*`.
- **`stream`** — canal SSE (`/api/{uuid}/stream`).
- **`version`** — versão da aplicação (sem `{uuid}`).
- **`auth`** — login e emissão de token (`POST /login`, token opaco rotativo; sem `{uuid}`) — única fatia-base, as demais podem depender dela mas nunca o contrário.

### Fatia do agregado `User` — `br.cdb.feature.user.*`
Pacote raiz com **`UserUseCase`** (único use case da fatia: todo `*Resource` que atua sobre dados do usuário injeta só ele; orquestra use cases de contexto + serviços de feature + SSE), `UserService`, `UserGuards` (guarda de propriedade/anti-IDOR). Subpacotes:
- **`profile`** — self-service `/api/me` (nome + preferências write-through); `profile.api` tem os DTOs HTTP, `profile.preference` tem `Preferences`/`PreferencesRepository` — aninhados sob `profile` para contar como uma única fatia perante o ArchUnit.
- **`seed`** — provisionamento inicial (usuário + categorias default).

### Plataforma — `br.cdb.core` (módulo `br-application`)
Autenticação/autorização (token opaco rotativo, `OwnershipFilter`), observabilidade (log de requisição + MDC), persistência JSON, config HTTP/OpenAPI, `ContextBridge` (costura CDI↔`Registry`).

### Framework comum — `br.commons` (módulo `br-commons`)
`Result` (Success/Failure), `MessageBus`, Logger próprio (console + arquivo rotativo), abstrações de persistência (JSON + pool JDBC), leitor YAML, `PdfBoxTextExtractor`, utilidades multiplataforma. Sem dependência de `br.cdb.*` — ver `br-commons/src/main/java/CLAUDE.md`.

---

## Índice de Referências

| Documento | Conteúdo |
|---|---|
| `README.md` | Visão geral, como executar (Maven/Docker), stack, configuração |
| `docs/functional_decomposition.md` | Decomposição por feature, em detalhe¹ |
| **`br-parent/CLAUDE.md`** | **Pom pai** — versões/deps herdadas, build compartilhado (compilador, PMD/CPD, Quarkus plugin), gotchas conhecidos de empacotamento (`web/` não copiado, `docker-compose` com Dockerfile fora do lugar) |
| **`br-commons/src/main/java/CLAUDE.md`** | **Framework comum** — índice de pacotes (`Result`, `Registry`, `MessageBus`, logger, JDBC/JSON/YAML, PDF, plataforma), sem dependência de `br.cdb.*` |
| **`br-context-people/src/main/java/CLAUDE.md`** | **Contexto people** — estrutura, o que NÃO está aqui (adapter/testes ficam em `br-application`), estado atual (não consumido) |
| **`br-context-monetary/src/main/java/CLAUDE.md`** | **Contexto monetary** — estrutura, modelos/use-cases reais, pontos não óbvios (`TransactionPolicy`, cartão como entidade) |
| **`br-application/src/main/java/CLAUDE.md`** | **Diretrizes backend (borda HTTP/CDI)** — índice operacional: VSA/Hexagonal, Result, Lombok, Null-Safety, Testes/ArchUnit, Qualidade & Build |
| **`web/CLAUDE.md`** | **Diretrizes frontend** — estilo + padrões 001–006 (request tracing, token rotativo, namespace de usuário, preferências server-owned) |
| `docs/backend/hexagonal-architecture.md` | Camadas Resource → UseCase → Service → Repository |
| `docs/backend/result-pattern.md` | Result / Railway + desembrulho `.get()`/fatal nos adapters |
| `docs/backend/null-safety.md` | JSpecify `@NullMarked` + NullAway/ErrorProne |
| `docs/backend/lombok.md` | `val` e suas exceções |
| `docs/backend/persistence-jdbc.md` | JDBC/H2, schema = mermaid, ciclo de vida do DB de dev, colunas anuláveis |
| `docs/backend/quality-and-build.md` | Gate PMD/CPD, gotchas de build Java 25, verificação sem `mvn`, setup de IDE |
| `docs/frontend/api-web.md` | Integração do frontend com a API REST |
| `docs/architecture-backend.mermaid` | **Pacotes (backend)** — placement de classes por pacote + dependências permitidas/proibidas |
| `docs/architecture-backend-classes.mermaid` | **Classes (backend)** — VSA + Hexagonal materializados numa fatia exemplar (transactions/monetary) |
| `docs/architecture-backend-activity.mermaid` | **Atividade (backend)** — fluxo de uma requisição mutadora: filtros → Resource → Facade → UseCase → Result |
| `docs/architecture-frontend.mermaid` | **Pacotes (frontend)** — placement de módulos por camada + regra de dependência |
| `docs/architecture-frontend-activity.mermaid` | **Atividade (frontend)** — ação do usuário → service → http-client (fila/token) → API, + canal SSE |
| `docs/db-ctx-people.mermaid` · `docs/db-ctx-monetary.mermaid` · `docs/db-features.mermaid` | **Schema ER canônico** (fonte da verdade do banco) |
| `docs/extrato/` | PDFs de exemplo (BTG, Santander) para testar a importação |

¹ Predata a migração JSON→JDBC e a realocação de Category/Tag para features de usuário; onde divergir do código/mermaid, **o código/mermaid vence**.

---

## Onde está o quê

- **Convenções de código (backend)** → `docs/backend/*` (índice em `br-application/src/main/java/CLAUDE.md`; cada módulo tem seu próprio `CLAUDE.md` — ver tabela acima).
- **Arquitetura e regras (frontend)** → `web/CLAUDE.md`.
- **Arquitetura geral (backend/frontend)** → `docs/architecture-*.mermaid` (pacotes, classes, atividade).
- **Schema do banco** → `docs/db-*.mermaid` (canônico; `Database` conforma).
- **Gotchas de build / IDE / qualidade** → `docs/backend/quality-and-build.md`.
