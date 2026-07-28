# CDB Finance — Guia Central

Gestor de finanças pessoais. Backend **Java 25 + Quarkus** (JVM mode); frontend **SPA Vanilla JS/CSS + jQuery 4** (servido pelo próprio backend). Arquitetura híbrida: **Vertical Slice** nas features de entrega HTTP sobre **Hexagonal** nos contextos de negócio (`br.cdb.context.*`) — features falam com contextos **apenas via os use cases expostos pela Facade** (`MonetaryUseCases.uc*()`). Cada feature é uma fatia numerada `br.cdb.feature.fNNN`, **hexágono auto-contido** (`_0_domain`/`_1_application`/`_2_infrastructure`: modelos+portas / use case+serviços+eventos / Resource+DTOs+adapter+módulo CDI próprios); os `*Resource` injetam só o use case da própria fatia (nunca um god-object central) e fazem só tradução de formato. Cross-feature é via eventos de domínio best-effort (`br.commons.MessageBus`), nunca import direto de serviço/use case de fatia irmã. Persistência: **100% JDBC/H2** (dev: file `./database`; teste: in-memory). Rotas de dados escopadas por `/api/{uuid}/…` com guarda de propriedade (anti-IDOR, `UserGuards` em `f000`).

> Este é o índice central. As diretrizes detalhadas vivem nos `CLAUDE.md` de cada módulo (`br-parent/`, `br-commons/src/main/java/`, `br-context-people/src/main/java/`, `br-context-monetary/src/main/java/`, `br-application/src/main/java/`, `web/`) e em `docs/`.
> **Schema do banco: os diagramas `docs/*.mermaid` são a fonte da verdade** — o código (DDL em `Database`) conforma ao diagrama, nunca o inverso.
> **Mapa de URLs por fatia**: `br-application/src/main/java/br/cdb/feature/package-info.java` lista todo endpoint exportado, agrupado por `fNNN` — é o inventário mais preciso e fica ao lado do código.

---

## Decomposição Funcional

### Módulos Maven (empacotamento físico)
`br-parent` (pom pai — versões, plugins, gate de qualidade; ver `br-parent/CLAUDE.md`) · `br-commons` (framework comum, sem `br.cdb.*`) · `br-context-people` + `br-context-monetary` (contextos hexagonais, dependem só de `br-commons`) · `br-application` (borda HTTP/CDI — `feature`/`core`/`infra` — depende dos três anteriores). `web/` (frontend) não é módulo Maven, mas o pom de `br-application` o copia para `META-INF/resources` (por isso o backend serve a SPA). Cada módulo tem seu próprio `CLAUDE.md` operacional. Versão única `1.0.0` em todos.

### Contextos de negócio — `br.cdb.context.*` (Hexagonal, livre de framework; DI via `Registry`)
- **`monetary`** (módulo `br-context-monetary`) — lógica financeira. Facade `MonetaryUseCases` (acessores estáticos para os use cases); modelos `Account`, `Balance`, `CostCenter`, `CreditCard`, `Statement`, `Transaction`; use cases `AccountUseCase`, `CreditCardUseCase`, `CostCenterUseCase`, `TransactionUseCase`.
- **`people`** (módulo `br-context-people`) — identidade mínima. Facade `PeopleContext` (montada por `PeopleBootstrap`); modelo `Person` (id/name/locale/language); `PersonUseCase`. **Não** inclui `User` (login) nem `Preferences` — esses são agregados de `br-application` (`br.cdb.core.security.User` e `br.cdb.feature.f001._0_domain.Preferences`), não deste contexto. É consumido por `f000` (`UserService`, ao criar usuário) e `f001` (`ProfileService`), ambos instanciando `PersonUseCase` diretamente (`new`) — que resolve o `PersonService` no `Registry`. Nenhuma feature injeta a Facade `PeopleContext` em si; o acesso por use case é permitido pela regra ArchUnit `feature_must_access_context_only_via_facade_or_domain_model`.
- **Núcleo comum entre contextos**: não existe um contexto `shared` — o pacote `context..shared..` é apenas reservado nas regras ArchUnit, sem implementação. O vocabulário compartilhado hoje (erro/evento de domínio) é `br.commons.business.{BusinessError,BusinessEvent,BusinessException}`, em `br-commons`.

### Features de entrega — `br.cdb.feature.fNNN` (hexágono próprio cada)
Existem hoje: `f000`–`f006`, `f009`, `f999`. O número expressa **ordem de criação** e é regra viva no ArchUnit (`feature_slices_depend_only_on_earlier_ones`): uma fatia só depende de fatias com número menor. Dependência "para cima" resolve-se por inversão — a fatia anterior define a porta em `_0_domain` e a posterior implementa (`f000.AccountOwnership` ⇐ `f002`; `f002.TransactionAccountOverlay` e `f004.TransactionCategoryOverlay` ⇐ `f005`).

- **`f000`** — fatia-base (todas as demais podem depender dela; ela não depende de nenhuma feature). Estrutura plana `_0/_1/_2`, sem sub-pacotes por assunto: SSE (`SSE`, `SseService`, `SseResource` — `/api/{uuid}/stream`), vocabulário de exclusão (`DeletionStrategy`/`DeletionOutcome`/`Deletions`, contrato uniforme), auth (`LoginResource` — `POST /login`, token opaco rotativo), `UserGuards` (guarda de propriedade/anti-IDOR + locale/language default), `UserService` (cria `Person` + `User` e publica `UserEvents.Created`), catálogo de centros de custo (`GET /api/cost-center`, sem `{uuid}`), `version` (`GET /api/version`), `closing` (`ClosingService`/`ClosingRepository`/`ClosingJDBCRepository`/`ClosingResource` — `/api/{uuid}/accounts/closing`; gate de política síncrono consumido por f005). Também abriga o evento cross-feature `TransactionsDeleted` (mora na base, não em quem publica).
- **`f001`** — self-service `/api/me` (nome + preferências write-through); `Profile`/`Preferences`/`PreferencesRepository`.
- **`f002`** — contas, com **cards** e **balance** fundidos aqui (fatias somente-leitura/passthrough do contexto, sem modelo/repositório próprio — não justificavam hexágono à parte): CRUD de conta, CRUD de cartão, saldo por período (`?period=yyyyMM`/`?year=yyyy`). Overlay `PERSON_ACCOUNT` guarda só a cor. Publica `AccountEvents`; o dispatch SSE é responsabilidade única de `f999`.
- **`f003`** — tags (classificação livre/transversal); mudanças propagadas via SSE; reage a `TransactionsDeleted` (`TagTransactionListener`) para purgar vínculos e a `TagDeleted` para o contrato de exclusão.
- **`f004`** — categorias (macro/micro); mudanças propagadas via SSE. **Semeia as categorias padrão** reagindo a `UserEvents.Created` (publicado por `f000.UserService`) — o seed não é do `f999`.
- **`f005`** — lançamentos e transferências: créditos, débitos, parcelas, filtros, patch de status, delete unitário/em grupo/transferência. Publica `TransactionsDeleted` após excluir transações — reagido best-effort por `f003` (tags) e pelo próprio `f005` (limpeza do overlay).
- **`f006`** — importação de extrato/fatura (preview→confirm), parsers **BTG** e **Santander** (cartão + conta), expansão de parcelas, sugestão de categoria, casamento de cartão.
- **`f009`** — dashboard: resultado mensal agregado (receitas/despesas/líquido), sem overlay nem `_0_domain` próprio.
- **`f999`** — última fatia (pode depender de todas), sem HTTP. Duas responsabilidades: (a) provisionamento no startup — cria o usuário `admin`/`admin` via `f000.UserService`, o que dispara em cascata o seed de categorias em `f004`; (b) **único dono do dispatch SSE** — `AccountStreamListener`/`CategoryStreamListener`/`TagStreamListener` reagem aos eventos de f002/f003/f004 e chamam `SSE.dispatch`.

### Plataforma — `br.cdb.core` (módulo `br-application`)
Autenticação/autorização (token opaco rotativo, `OwnershipFilter`), observabilidade (log de requisição + MDC), erro HTTP (`ProblemDetail` + `ExceptionMapper`s), config HTTP/OpenAPI, `SpaFallbackRoute` (deep-link da SPA → `index.html`), `ContextBridge` (costura CDI↔`Registry`: produz o `DataSource`, aplica as migrações one-shot, cria o schema e publica os adaptadores JDBC nas portas dos contextos no `StartupEvent`).

### Framework comum — `br.commons` (módulo `br-commons`)
`Result` (Success/Failure), `MessageBus`, Logger próprio (console + arquivo rotativo), abstrações de persistência (pool JDBC + stack JSON), leitor YAML, `PdfBoxTextExtractor`, utilidades multiplataforma. Sem dependência de `br.cdb.*` — ver `br-commons/src/main/java/CLAUDE.md`.

---

## Persistência — estado atual

**Tudo em JDBC/H2.** Não há mais nenhum agregado em JSON:
- `Closing` → tabela `PERSON_PREFERENCES` (`ClosingJDBCRepository`, em `f000/_2_infrastructure/persistence`).
- Catálogo de centros de custo → tabela `MON_COST_CENTER`, semeada no próprio DDL de `Database`.
- Adaptadores das portas de **contexto** ficam em `br.cdb.infra.persistence`; adaptadores de **feature** ficam no `fNNN/_2_infrastructure/persistence` da própria fatia.

O stack JSON de `br.commons` sobrevive só como resíduo: `br.cdb.core.persistence.JsonStorageConfig` ainda produz um bean `Storage` (`LocalFileStorage`) e `JsonStorageProperties`/`STORAGE_JSON_PATH` ainda existem, mas **sem nenhum consumidor**. `JsonStorageConfig` continua necessário pelo outro papel: é o `ObjectMapperCustomizer` que registra os módulos Jackson (BigDecimal com 2 casas, enums de transação em lowercase).

---

## Índice de Referências

| Documento | Conteúdo |
|---|---|
| `README.md` | Visão geral, como executar (Maven/Docker), stack, configuração |
| `br-application/…/br/cdb/feature/package-info.java` | **Inventário de URLs por fatia** — todo endpoint exportado, agrupado por `fNNN` |
| **`br-parent/CLAUDE.md`** | **Pom pai** — versões/deps herdadas, build compartilhado (compilador, PMD/CPD, Quarkus plugin) |
| **`br-commons/src/main/java/CLAUDE.md`** | **Framework comum** — índice de pacotes (`Result`, `Registry`, `MessageBus`, logger, JDBC/JSON/YAML, PDF, plataforma), sem dependência de `br.cdb.*` |
| **`br-context-people/src/main/java/CLAUDE.md`** | **Contexto people** — estrutura, o que NÃO está aqui (adapter/testes ficam em `br-application`) |
| **`br-context-monetary/src/main/java/CLAUDE.md`** | **Contexto monetary** — estrutura, modelos/use-cases reais, pontos não óbvios (`TransactionPolicy`, cartão como entidade) |
| **`br-application/src/main/java/CLAUDE.md`** | **Diretrizes backend (borda HTTP/CDI)** — índice operacional: VSA/Hexagonal, Result, Lombok, Null-Safety, Testes/ArchUnit, Qualidade & Build |
| **`web/CLAUDE.md`** | **Diretrizes frontend** — estilo + padrões 001–006 (request tracing, token rotativo, namespace de usuário, preferências server-owned) |
| `docs/backend/hexagonal-architecture.md` | Camadas Resource → UseCase → Service → Repository |
| `docs/backend/result-pattern.md` | Result / Railway + desembrulho `.get()`/fatal nos adapters |
| `docs/backend/null-safety.md` | JSpecify `@NullMarked` + NullAway/ErrorProne |
| `docs/backend/lombok.md` | `val` e suas exceções |
| `docs/backend/persistence-jdbc.md` | JDBC/H2, schema = mermaid, ciclo de vida do DB de dev, colunas anuláveis, migrações one-shot |
| `docs/backend/quality-and-build.md` | Gate PMD/CPD, gotchas de build Java 25, verificação sem `mvn`, setup de IDE |
| `docs/frontend/api-web.md` | Integração do frontend com a API REST |
| `docs/architecture-backend.mermaid` | **Pacotes (backend)** — placement de classes por pacote + dependências permitidas/proibidas |
| `docs/architecture-backend-classes.mermaid` | **Classes (backend)** — VSA + Hexagonal materializados numa fatia exemplar (transactions/monetary) |
| `docs/architecture-backend-activity.mermaid` | **Atividade (backend)** — fluxo de uma requisição mutadora: filtros → Resource → Facade → UseCase → Result |
| `docs/architecture-frontend.mermaid` | **Pacotes (frontend)** — placement de módulos por camada + regra de dependência |
| `docs/architecture-frontend-activity.mermaid` | **Atividade (frontend)** — ação do usuário → service → http-client (fila/token) → API, + canal SSE |
| `docs/db-ctx-people.mermaid` · `docs/db-ctx-monetary.mermaid` · `docs/db-features.mermaid` | **Schema ER canônico** (fonte da verdade do banco) |
| `docs/extrato/` | PDFs de exemplo (BTG, Santander) para testar a importação |
| `.claude/refactor.md` | Histórico da migração fatias-planas→`fNNN`. **Não versionado** (`.claude/` está no `.gitignore`) — só existe na máquina local |

---

## Onde está o quê

- **Endpoints por fatia** → `br-application/src/main/java/br/cdb/feature/package-info.java`.
- **Convenções de código (backend)** → `docs/backend/*` (índice em `br-application/src/main/java/CLAUDE.md`; cada módulo tem seu próprio `CLAUDE.md` — ver tabela acima).
- **Arquitetura e regras (frontend)** → `web/CLAUDE.md`.
- **Arquitetura geral (backend/frontend)** → `docs/architecture-*.mermaid` (pacotes, classes, atividade).
- **Schema do banco** → `docs/db-*.mermaid` (canônico; `Database` conforma).
- **Gotchas de build / IDE / qualidade** → `docs/backend/quality-and-build.md`.
