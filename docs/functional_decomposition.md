# Decomposição Funcional

CDB Finance — gestor de finanças pessoais (Java 25 + Quarkus). Arquitetura híbrida:
**Vertical Slice** nas features de entrega HTTP (`br.community.feature.*`) sobre **Hexagonal**
nos contextos de negócio (`br.community.context.*`). Features falam com os contextos apenas via
**Facade**. Persistência em **JDBC/H2** (dev: arquivo; teste: in-memory), com JSON remanescente para `Closing` e o catálogo de centros de custo. Rotas de usuário são escopadas por `/api/{uuid}/…`.

## 1. Contextos de Negócio (Hexagonal)

- [Monetário] Concentra a lógica financeira [Isolar regras de negócio]. Fachada `MonetaryContext`; camadas `_0_domain` / `_1_application` / `_2_infrastructure`. Modelos: `MonetaryAccount`, `MonetaryTransaction`, `MonetaryCenter`, `MonthlyBalance`, `MonetaryStatement`. (Categoria e Tag saíram do contexto monetário — hoje são features de usuário `categories`/`tags`.) Use cases: `AccountUseCase`, `TransactionUseCase`, `MetadataUseCase`.
- [Segurança] Gerencia usuário e preferências [Fonte da verdade de identidade]. Fachada `SecurityContext`; agregado `User` (`username`, `name`, `password`, `Preferences`). `Preferences`: `theme`, `language`, `locale`, `sidebarCollapsed` (merge parcial via `PreferencesPatch`). Use case `UserUseCase`.
- [Compartilhado] Núcleo comum dos contextos [Evitar acoplamento]. `DomainEvent`, `DomainError`, `DomainException`, `SharedModule`.

## 2. Features de Usuário (`/api/{uuid}/…`)

- [Contas] CRUD de contas e limites de crédito; cartões como sub-recurso [Manter saldos]. `AccountResource` projeta `MonetaryAccount` + saldo + limite (`AccountLimit`) + cartões (`Card`, via `CardResource` em `/accounts/{accountId}/cards`). Sub-recursos abaixo. Exclusão de conta/cartão com transações vinculadas segue o contrato uniforme (ver nota abaixo); "Inativar" (PATCH `active=false`, já existente) é sempre uma alternativa oferecida no diálogo do frontend.
  - [Saldo] Consulta saldo por período [Conferir posição mensal/anual]. `AccountBalanceResource` (`?period=yyyyMM` ou `?year=yyyy`).
  - [Fechamento] Define período de fechamento [Consolidar competência em aberto]. `ClosingResource` (GET/POST/DELETE).
  - [Extrato] Exibe histórico mensal por conta [Facilitar conferência]. `StatementResource`, `StatementService` (filtro `?status=`).
  - [Importação de Extratos] Lê PDF de banco/cartão [Automatizar lançamentos]. Fluxo preview→confirm (`StatementImportResource`). Detecta tipo (`DocumentTypeDetector`) e emissor (`IssuerDetector`); parsers por banco BTG e Santander (cartão + conta), `BankStatementParserRegistry`, `CreditCardStatementParserRegistry`; expande parcelas (`InstallmentExpander`), sugere categoria (`CategoryGuesser`), casa cartão (`CardMatcher`) e resolve a conta real de destino a partir do cartão (`MonetaryCardProvider`) — cartão não tem saldo próprio, a fatura é postada na conta a que pertence.
  - [Transações] Registra créditos, débitos, transferências e parcelas [Rastrear fluxo]. `TransactionResource` (filtros, `transfer`, patch de status, delete unitário/em grupo via `mode`).
- [Categorias] CRUD de categorias [Análise macro]. `CategoryResource`; propaga mudanças via SSE (`CategoryStreamListener`). Exclusão com transações vinculadas segue o contrato uniforme (abaixo); não há mais reatribuição automática para "Outros" (removida). `active` agora é funcional: categoria inativa (ou com ancestral inativo) some dos dropdowns de classificação, mas o histórico permanece intacto; "Inativar" é oferecida como alternativa no diálogo de exclusão, com botão de reativação na lista.
- [Tags] CRUD de rótulos livres [Análise micro / marcação transversal]. `TagResource`; SSE via `TagStreamListener`. Exclusão com transações vinculadas segue o contrato uniforme (abaixo), com uma 3ª opção exclusiva de tag: apenas desvincular (`DETACH`), sem tocar nas transações.

> **Contrato uniforme de exclusão** (contas, cartões, categorias, tags): sem vínculos → `204`; com transações vinculadas → `409` (`code=LINKED_TRANSACTIONS`, `count` = quantidade) a menos que o cliente informe `?strategy=MOVE&targetId={uuid}` (reatribui) ou `?strategy=DELETE` (apaga entidade + transações); tags aceitam também `?strategy=DETACH` (desvincula sem apagar transação). O frontend reage ao `409` com um diálogo (`linkedDeleteDialog`) oferecendo as estratégias válidas para o recurso — cartão, conta e categoria somam ainda "Inativar" como alternativa (fora do contrato de exclusão, via PATCH).
- [Dashboard] Agrega resultado mensal [Visão geral]. `DashboardResource`, `DashboardService` (receitas/despesas/líquido por categoria).
- [Perfil] Lê e atualiza o próprio usuário [Self-service sem risco de IDOR]. `SelfResource` (GET/PATCH `/api/me`); atualiza nome e/ou preferências (write-through de tema e estado da sidebar).

## 3. Features de Sistema (sem `{uuid}`)

- [Centro de Custo] Catálogo somente-leitura [Organizar rateios]. `CostCenterResource`, `CostCenterCatalog` (`GET /api/cost-center`).
- [Streaming/SSE] Push de eventos em tempo real [Atualizar interface sem polling]. `SseController`, `SseService` (`GET /api/{uuid}/stream`).
- [Versão] Expõe versão do build [Exibir no rodapé/sidebar]. `VersionResource` (`GET /api/version`, lê `pom.xml`).

## 4. Core / Plataforma (`br.community.core`)

- [Autenticação] Login e emissão de token [Proteger acesso]. `LoginResource` (`POST /login`), `AccessTokenStore` (JWT), `CurrentUser`, `UserDetailsServiceImpl`, `UserSeeder`, `SecurityConfig`.
- [Autorização] Filtros e guarda de propriedade [Bloquear acesso indevido]. `AuthenticationFilter`, `AuthorizationFilter`, `OwnershipInterceptor` (valida `{uuid}` do dono).
- [Observabilidade] Log de requisições e correlação [Rastrear chamadas]. `RequestLoggingFilter`, `MDCLoggingFilter`.
- [Persistência] JDBC/H2 para os dados de negócio (`DataSourceProperties`; adaptadores `*JDBCRepository` em `br.community.infra.persistence`); JSON remanescente (`JsonStorageConfig`, `*JsonRepository`) para `Closing` e o catálogo de centros de custo [Manter estado].
- [Web/API] Configuração HTTP transversal [Padronizar contrato]. `OpenApiConfig`, `WebConfig`, `GlobalExceptionHandler`, validação `@TwoDecimalPlaces`.

## 5. Framework Comum (`br.commons`)

- [Resultado/Erros] `Result` (Success/Failure), `RT` [Fluxo sem exceção].
- [Mensageria] `MessageBus`, `Message*` [Eventos internos].
- [Log] Logger próprio com canais (console, arquivo rotativo) e ponte SLF4J/JUL [Diagnóstico].
- [Persistência] Abstrações JSON, pool JDBC e in-memory [Infra de storage plugável].
- [Serialização] Leitor/escritor YAML [Configuração].
- [PDF] `PdfBoxTextExtractor` [Base da importação de extratos].
- [Plataforma/Tools] `Platform`, `Terminal`, `FileSystem` (Linux/Windows), `Dates`/`Time`, `Strings`, `Parser` [Utilidades multiplataforma].
