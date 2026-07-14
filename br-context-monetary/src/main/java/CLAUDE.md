# br-context-monetary — Contexto Monetário

Módulo Maven `context-monetary` (`br.cdb.contexts:context-monetary`, artifactId `context-monetary`). Contexto de negócio hexagonal, **livre de framework** — sem Quarkus/CDI/Spring, depende só de `br-commons`. Concentra toda a lógica financeira: contas, cartões, transações, saldos e centros de custo.

> Índice operacional deste módulo. Visão geral da arquitetura híbrida VSA+Hexagonal em `@CLAUDE.md` (raiz) e `@docs/backend/hexagonal-architecture.md`. Schema de banco (fonte da verdade) em `@docs/db-ctx-monetary.mermaid`.

## Estrutura

```
br.cdb.context.monetary
├── _0_domain
│   ├── model/       Account, AccountBalance, CostCenter, CreditCard, Statement, Transaction
│   ├── event/       TransactionEvents (Created / Updated / Deleted)
│   └── repository/  AccountRepository, BalanceRepository, CardRepository, CostCenterRepository, TransactionRepository  (portas)
├── _1_application
│   ├── command/     AccountCommand, CardCommand, CostCenterCommand, TransactionCommand,
│   │                ImportedTransactionCommand, ImportConfirmCommand, TransactionPolicy
│   ├── event/       AccountEventListener (placeholder, sem listeners registrados), TransactionEventListener
│   ├── service/     AccountService, BalanceService, BalanceRecalculationService, CardService,
│   │                CostCenterService, TransactionService
│   └── usecase/     AccountUseCase, CardUseCase, MetadataUseCase, TransactionUseCase
└── MonetaryContext.java     (Facade — único ponto de entrada externo)
```

* **`_0_domain`** — modelos imutáveis (`record`) e portas de repositório. Sem dependência de infraestrutura. `CostCenter` é o modelo de domínio para "centro de custo" — não confundir com o catálogo global somente-leitura da feature de sistema `br.cdb.feature.system.costcenter` (persistido em JSON, é outra coisa).
* **`_1_application`** — `service/` encapsula acesso a repositório + validação de domínio (parâmetros simples); `usecase/` orquestra services e publica eventos (recebe `Command`s); `event/` reage a eventos via `@MessageListener` (`MessageBus`, de `br-commons`).
* **`MonetaryContext`** — `implements br.commons.annotation.Facade`; único ponto de acesso permitido a partir de `br-application` (ArchUnit `feature_must_access_context_only_via_facade_or_domain_model`). Agrupa 5 áreas: contas, saldos, transações, centros de custo, cartões.
* **Sem composition root separado** — `Service`/`UseCase`/`MonetaryContext` se auto-conectam via `Registry.tryGet`/`Registry.get` nos próprios inicializadores de campo (sem construtor explícito, sem `MonetaryBootstrap`). `MonetaryContext.instance()` é o ponto de entrada: assina `TransactionEventListener` no `MessageBus` na primeira chamada e publica a Facade no `Registry`. Chamado por `br.cdb.core.ContextBridge.monetaryContext(...)` no startup — que só precisa registrar as 5 portas de repositório antes.

## Pontos não óbvios

* **`AccountEventListener` é um placeholder vazio** (`private AccountEventListener() {}`, sem métodos `@MessageListener`) — existe o pacote/classe mas nada está registrado nele. Só `TransactionEventListener` é assinado (via `MonetaryContext.instance()`, no primeiro acesso) hoje (reage a `TransactionEvents.Created/Updated/Deleted` acionando `BalanceRecalculationService`).
* **`BalanceRecalculationService` usa `Registry.tryGet` (não `Registry.get`) para `AccountService`/`BalanceService`/`TransactionService`** — de propósito: como não há composition root, a ordem em que `CardUseCase`/`TransactionUseCase`/`TransactionEventListener` são construídos primeiro varia; `tryGet` deixa cada dependência se auto-registrar sob demanda em vez de exigir que outra classe já tenha "aquecido" o `Registry` antes. Trocar de volta para `get` reintroduz um `IllegalStateException` de startup dependente de ordem.
* **`TransactionPolicy`** (sealed: `Block`/`Move(targetId)`/`Purge`) — parâmetro de `deleteAccount`/`deleteCard` na Facade; decide o que fazer com transações vinculadas à entidade excluída. É passado de fora (a UI decide a política, não o contexto).
* **Cartão (`CreditCard`)** é entidade própria do contexto (`last4`, `accountId`, `active`) desde a migração cartão-como-entidade; limite de crédito/cheque especial e ciclo de fatura continuam sendo colunas de `Account` (compartilhadas por todos os cartões da conta) — ver `@docs/backend/persistence-jdbc.md`.
* **`AccountBalance`** — projeção de saldo por competência (`?period=yyyyMM`/`?year=yyyy`) exposta pela Facade (`getMonthlyBalance`/`getYearBalances`). Chave de negócio é o par `(accountId, period)` — o record não carrega `id` próprio; `BalanceRepository.delete(UUID, YearMonth)` (não `deleteById`) é o caminho real de exclusão usado por `BalanceService`/`BalanceRecalculationService`.
* A Facade **não valida política de usuário** (ex.: período de fechamento) — isso é fronteira da feature (`ClosingService.validateDate` em `br-application`), não deste contexto. O contexto aceita qualquer transação bem-formada.

## Testes

`src/test/java/br/cdb/context/monetary/_1_application/usecase/` — cobre os 4 use cases (`AccountUseCaseTest`, `CardUseCaseTest`, `MetadataUseCaseTest`, `TransactionUseCaseTest`) com JUnit 5 puro, sem Quarkus/CDI/Mockito. `InMemoryRepositories` (mesmo pacote, visibilidade padrão) implementa as 5 portas de repositório em memória.

* **Sem construtor a injetar** — como `Service`/`UseCase` se auto-conectam via `Registry` (não recebem dependências por construtor), o setup do teste não monta objetos com `new X(fakeRepo)`; ele publica os fakes no `Registry` (`Registry.set(AccountRepository.class, fakeRepo)`) e deixa o `new AccountUseCase()` (sem args) resolver sozinho.
* **`Registry` é global e persiste entre métodos de teste** — todo `@BeforeEach` começa com `Registry.remove(XService.class)` para cada `Service` que o use case toca, forçando reconstrução contra os fakes da rodada atual. Sem isso, o `Registry.tryGet` de um teste anterior devolveria um `Service` já resolvido e preso ao fake (ou repositório JDBC real, se um `@QuarkusTest` rodou antes no mesmo fork) de outra rodada.
* **Não passa pela `MonetaryContext`** — os testes constroem o use case sob teste diretamente; não chamam `MonetaryContext.instance()` (isso evita depender da assinatura do `TransactionEventListener` no `MessageBus`, que é cumulativa e nunca desfeita entre testes).

## O que NÃO está aqui

* **Adaptadores de persistência** — `AccountJDBCRepository`, `BalanceJDBCRepository` (dentro de `AccountJDBCRepository` ou correlato), `CardJDBCRepository`, `CostCenterJDBCRepository`, `TransactionJDBCRepository` vivem em `br-application` (`br.cdb.infra.persistence.monetary`), não neste módulo. O contexto só conhece as portas (`_0_domain.repository`).
* **ArchUnit e integração HTTP** — essa parte da suíte roda em `br-application/src/test/java/br/cdb/context/monetary/**` (precisa de CDI/Quarkus ou do classpath completo da borda HTTP) — ver `@br-application/src/main/java/CLAUDE.md`.
* **Importação de extrato/PDF (parsers BTG/Santander, casamento de cartão, sugestão de categoria)** — é lógica de feature (`br.cdb.feature.user.accounts.transactions.importer`), não deste contexto. O contexto só recebe o resultado já traduzido via `createImportedTransaction(ImportedTransactionCommand)`.

## Convenções

Mesmas do backend em geral: `@NullMarked` obrigatório (exceto enum), `val` em locais finais, `record` para modelos/commands, `Result<T, BusinessError>` (de `br-commons`) em toda operação que possa falhar — ver `@docs/backend/result-pattern.md`, `@docs/backend/null-safety.md`, `@docs/backend/lombok.md`.

## Dependências (pom)

Depende só de `br.cdb:commons`. Não depende de `context-people` nem de `br-application` — a relação de dependência é sempre de fora (`br-application`) para dentro.
