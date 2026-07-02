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

- [Contas] CRUD de contas e cartões [Manter saldos]. `AccountResource` (filtro `?type=card`); projeta `MonetaryAccount` + saldo. Sub-recursos abaixo.
  - [Saldo] Consulta saldo por período [Conferir posição mensal/anual]. `AccountBalanceResource` (`?period=yyyyMM` ou `?year=yyyy`).
  - [Fechamento] Define período de fechamento [Consolidar competência em aberto]. `ClosingResource` (GET/POST/DELETE).
  - [Extrato] Exibe histórico mensal por conta [Facilitar conferência]. `StatementResource`, `StatementService` (filtro `?status=`).
  - [Importação de Extratos] Lê PDF de banco/cartão [Automatizar lançamentos]. Fluxo preview→confirm (`StatementImportResource`). Detecta tipo (`DocumentTypeDetector`) e emissor (`IssuerDetector`); parsers por banco BTG e Santander (cartão + conta), `BankStatementParserRegistry`, `CreditCardStatementParserRegistry`; expande parcelas (`InstallmentExpander`), sugere categoria (`CategoryGuesser`), casa cartão (`CardMatcher`).
  - [Transações] Registra créditos, débitos, transferências e parcelas [Rastrear fluxo]. `TransactionResource` (filtros, `transfer`, patch de status, delete unitário/em grupo via `mode`).
- [Categorias] CRUD de categorias [Análise macro]. `CategoryResource`; propaga mudanças via SSE (`CategoryStreamListener`).
- [Tags] CRUD de rótulos livres [Análise micro / marcação transversal]. `TagResource`; SSE via `TagStreamListener`.
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
