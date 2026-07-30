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
Existem hoje: `f000`–`f006`, `f009`, `f999`. O número expressa **ordem de criação**, não mais uma restrição de dependência: a regra viva no ArchUnit é `feature_slices_must_not_depend_on_sibling_slices` — **fatia de negócio não importa fatia de negócio irmã**, nem para cima nem para baixo. Só duas exceções, por papel arquitetural: alvo `f000` (kernel compartilhado) sempre permitido; origem `f999` (composition root) sempre permitida. Toda comunicação cross-slice passa por um destes mecanismos: **evento** (`br.commons.MessageBus`, record declarado em `f000._0_domain.event`) para efeito sem retorno — escrita, cascata de exclusão (`TransactionsDeleted`/`AccountDeleted`/`CategoryDeleted`/`TagDeleted`/`CategoryReassigned`/`TransactionImported`); **`f000.InternalApi`** (HTTP real contra o endpoint público da fatia dona, autenticado por token efêmero de `AccessTokenStore`, nunca o token de sessão) para leitura síncrona que precisa de retorno; **adapter em `f999._2_infrastructure.adapter`** implementando uma porta declarada pelo consumidor em seu `_0_domain` — hoje só `DeletionQueueAdapter` (liga `f002.DeletionQueue` a `f999.DeletionQueueService`), único caso vivo desde que a fase 4 matou os 4 anteriores (ver `.claude/refactor.md`). `feature_slices_depend_only_on_earlier_ones` continua como rede de segurança barata, mas não é mais o mecanismo de desacoplamento. Contextos de negócio próprios (`br-context-monetary`/`br-context-people`, módulos Maven separados) foram **dissolvidos na fase 2**: as classes (Account/Balance/CreditCard/Transaction/CostCenter/Person e seus use cases/services) viraram subpacotes Registry-wired dentro da fatia dona do assunto (`fNNN._0_domain.{model,repository,event}`, `fNNN._1_application.{service,usecase}`), sem Facade — acesso via `Registry.tryGet(XUseCase.class)` direto.

- **`f000`** — fatia-base (todas as demais podem depender dela; ela não depende de nenhuma feature). Estrutura plana `_0/_1/_2`, sem sub-pacotes por assunto: SSE (`SSE`, `SseService`, `SseResource` — `/api/{uuid}/stream`), vocabulário de exclusão (`DeletionStrategy`/`DeletionOutcome`/`Deletions`, contrato uniforme), auth (`LoginResource` — `POST /login`, token opaco rotativo; `TOKEN_HEADER` mora em `core.web.HTTPRequest`, não aqui), `UserGuards` (guarda de propriedade/anti-IDOR — hoje lê direto `f002.AccountUseCase`/`f003.CreditCardUseCase`, um dos últimos acessos cross-slice remanescentes), `UserService` (cria `Person` + `User` e publica `UserEvents.Created`), `InternalApi` (helper de leitura cross-slice síncrona via HTTP real — ver acima), catálogo de centros de custo (`GET /api/cost-center`, sem `{uuid}`), `version` (`GET /api/version`), `closing` (`ClosingService`/`ClosingRepository`/`ClosingJDBCRepository`/`ClosingResource` — `/api/{uuid}/accounts/closing`; gate de política síncrono consumido por f006), `Person`/`PersonUseCase`/`CostCenter` (ex-contexto, dissolvidos aqui na fase 2). Também abriga vocabulário cross-feature que precisa ser referenciável por qualquer fatia: `TransactionsDeleted`, `AccountStreamEvents`, `CategoryReassigned`, `TransactionImported` (eventos de domínio).
- **`f001`** — self-service `/api/me` (nome + preferências write-through); `Profile`/`Preferences`/`PreferencesRepository`.
- **`f002`** — contas, com **balance** fundido aqui (fatia fina demais para hexágono próprio): CRUD de conta, saldo por período (`?period=yyyyMM`/`?year=yyyy`). `Account`/`Balance` (ex-contexto monetário, dissolvidos aqui na fase 2) ganharam `COD_PERSON` nativo na fase 3 (guarda anti-IDOR implícita — `findAllByPerson`/`findByIdAndPerson`); `F002_BALANCE.FLG_DIRTY` (fase 5) é consumido pelo job de reconciliação de `f999`. `AccountResponse` embute `cards[]` via uma projeção somente-leitura própria (`AccountResponse.Card`, lendo `f003.CreditCardUseCase` direto) — a mutação de cartão é de `f003`. Publica `AccountStreamEvents`; o dispatch SSE é responsabilidade única de `f999`. Declara a porta `DeletionQueue` (implementada por `f999.DeletionQueueAdapter`) pra gravar na fila de retry ao apagar conta.
- **`f003`** — cartões de crédito (`/api/{uuid}/accounts/{accountId}/cards`). Extraída de `f002` (era passthrough fundido, cresceu até justificar fatia própria — ver `.claude/refactor.md`); ganhou `_0_domain`/módulo CDI próprio na fase 2 (hospeda `CreditCard`/`CreditCardUseCase`, ex-contexto monetário, com `COD_PERSON` nativo desde a fase 3).
- **`f004`** — tags (classificação livre/transversal); mudanças propagadas via SSE; reage a `TransactionsDeleted` (`TagTransactionListener`) para purgar vínculos e a `TagDeleted` para o contrato de exclusão.
- **`f005`** — categorias (macro/micro); mudanças propagadas via SSE. **Semeia as categorias padrão** reagindo a `UserEvents.Created` (publicado por `f000.UserService`) — o seed não é do `f999`. Expõe `GET /categories/transfer?nature=` (endpoint interno, consumido via `InternalApi` por `f006`).
- **`f006`** — lançamentos, transferências **e importação de extrato/fatura** (fatia `f007` fundida aqui na fase 6: preview→confirm, parsers **BTG** e **Santander**, expansão de parcelas, sugestão de categoria, casamento de cartão — path `/api/{uuid}/accounts/transactions/import/*` preservado). Créditos, débitos, parcelas, filtros, patch de status, delete unitário/em grupo/transferência. Publica `TransactionsDeleted` após excluir transações — reagido best-effort por `f004` (tags) e pelo próprio `f006` (`TransactionOverlayListener`, limpeza do vínculo `F005_TRANSACTION_CATEGORY`); `TransactionImported` (import) e `CategoryReassigned` (reage, MOVE de categoria) no mesmo listener. Expõe `GET /accounts/transactions/by-category?categoryIds=` (endpoint interno, consumido via `InternalApi` por `f005`). **Sem `*UseCase` de fronteira**: a fatia foi dividida em par CQRS Registry-wired — `_1_application.usecase.ReadUseCases` (toda leitura, incl. guarda `ownsAccount` da listagem e as leituras cross-slice por `InternalApi`) e `WriteUseCases` (toda mutação, incl. política de usuário e publicação de SSE/cascata); os `*Resource` injetam os dois direto (produzidos por `F006Module`, que também publica `UserGuards`/`InternalApi` no `Registry` no `StartupEvent`).
- **`f009`** — dashboard: resultado mensal agregado (receitas/despesas/líquido), sem overlay nem `_0_domain` próprio. Lê transações via `InternalApi` contra `GET /accounts/transactions` de `f006` (fase 6) — não mais acesso direto à engine.
- **`f999`** — última fatia (pode depender de todas), sem HTTP. Quatro responsabilidades: (a) provisionamento no startup — cria o usuário `admin`/`admin` via `f000.UserService`, o que dispara em cascata o seed de categorias em `f005`; (b) **único dono do dispatch SSE** — `AccountStreamListener`/`CategoryStreamListener`/`TagStreamListener` reagem aos eventos de f002/f004/f005/f006 e chamam `SSE.dispatch`; (c) **composition root** — todo adapter que liga a porta de uma fatia ao provedor de outra vive em `f999._2_infrastructure.adapter` (hoje só `DeletionQueueAdapter`); (d) **fila de retry** (fase 5) — `DeletionQueueService.runOnce()` (`F999_DELETION_QUEUE`, `@Scheduled` a cada 5min via `DeletionQueuePurgeJob`) reprocessa evento cross-slice que falhou/o processo caiu antes de completar, e recomputa `F002_BALANCE` marcado sujo.

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
