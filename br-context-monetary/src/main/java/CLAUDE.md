# br-context-monetary — Contexto Monetário

Módulo Maven `context-monetary` (`br.cdb.contexts:context-monetary`, artifactId `context-monetary`). Contexto de negócio hexagonal, **livre de framework** — sem Quarkus/CDI/Spring, depende só de `br-commons`. Concentra toda a lógica financeira: contas, cartões, transações, saldos e centros de custo.

> Índice operacional deste módulo. Visão geral da arquitetura híbrida VSA+Hexagonal em `@CLAUDE.md` (raiz) e `@docs/backend/hexagonal-architecture.md`. Schema de banco (fonte da verdade) em `@docs/db-ctx-monetary.mermaid`.

## Estrutura

```
br.cdb.context.monetary
├── _0_domain
│   ├── model/       Account, Balance, CostCenter, CreditCard, Statement, Transaction
│   ├── event/       AccountEvents, CreditCardEvents, TransactionEvents (cada um Created / Updated / Deleted)
│   └── repository/  AccountRepository, BalanceRepository, CreditCardRepository, CostCenterRepository, TransactionRepository  (portas)
├── _1_application
│   ├── command/     AccountCommand, CreditCardCommand, CostCenterCommand, TransactionCommand,
│   │                TransactionScope, TransactionPolicy
│   ├── event/       TransactionEventListener (único listener registrado)
│   ├── service/     AccountService, BalanceService, CreditCardService, CostCenterService,
│   │                TransactionService
│   └── usecase/     AccountUseCase, CreditCardUseCase, CostCenterUseCase, TransactionUseCase
└── MonetaryUseCases.java     (Facade — acessores estáticos para os use cases)
```

* **`_0_domain`** — modelos imutáveis (`record`) e portas de repositório. Sem dependência de infraestrutura. `CostCenter` é o modelo de domínio para "centro de custo"; o endpoint global somente-leitura que o expõe (`GET /api/cost-center`, sem namespace de usuário) é `CostCenterResource`, da fatia-base `f000` — persistido na tabela `MON_COST_CENTER`, semeada no DDL de `Database`.
* **`_1_application`** — `service/` encapsula acesso a repositório + validação de domínio (parâmetros simples); `usecase/` orquestra services e publica eventos (recebe `Command`s); `event/` reage a eventos via `@MessageListener` (`MessageBus`, de `br-commons`).
* **`MonetaryUseCases`** — `implements br.commons.annotation.Facade`; ponto de acesso a partir de `br-application`, mas **sem métodos de delegação**: expõe só acessores estáticos `ucAccount()`/`ucCreditCard()`/`ucCostCenter()`/`ucTransaction()` que resolvem (e registram sob demanda) o use case no `Registry` via `tryGet`. O chamador trabalha direto com o use case devolvido (ArchUnit permite `_1_application.usecase` a partir de `feature`).
* **Sem composition root separado** — `Service`/`UseCase` se auto-conectam via `Registry.tryGet`/`Registry.get` nos próprios inicializadores de campo (sem construtor explícito, sem `MonetaryBootstrap`, sem instância única da Facade). A assinatura do `TransactionEventListener` no `MessageBus` acontece no **construtor de `TransactionUseCase`** — é o único construtor explícito do contexto, e existe só para isso (o `MessageBus` guarda os containers já assinados por FQN e ignora reassinaturas). As 5 portas de repositório precisam estar no `Registry` antes do primeiro acesso — em produção/`@QuarkusTest` isso é feito por `br.cdb.core.ContextBridge` no `StartupEvent`.

## Pontos não óbvios

* **`AccountEvents`/`CreditCardEvents` do contexto são write-only** — `AccountUseCase`/`CreditCardUseCase` publicam esses eventos no `MessageBus`, mas nada fora deste módulo sequer os importa (não há `AccountEventListener`). Só `TransactionEventListener` é assinado hoje: reage a `TransactionEvents.Created/Updated/Deleted` chamando `BalanceService.recalculate(accountId)` — **não existe** `BalanceRecalculationService`.
  ⚠️ **Homônimo perigoso:** `br.cdb.feature.f002._0_domain.event.AccountEvents` é uma classe *diferente*, da feature, e essa sim tem listener (`f999.AccountStreamListener`, que despacha SSE). Antes de concluir "ninguém escuta `AccountEvents`", confira o pacote do import.
* **Dependências resolvidas com `Registry.tryGet`, não `Registry.get`** — em `TransactionEventListener` e nos services que se auto-conectam. De propósito: como não há composition root, a ordem em que `CreditCardUseCase`/`TransactionUseCase`/`TransactionEventListener` são construídos primeiro varia; `tryGet` deixa cada dependência se auto-registrar sob demanda em vez de exigir que outra classe já tenha "aquecido" o `Registry` antes. Trocar para `get` reintroduz um `IllegalStateException` de startup dependente de ordem.
* **`TransactionPolicy`** (sealed: `Block`/`Move(targetId)`/`Purge`) — viaja dentro de `AccountCommand.Delete`/`CreditCardCommand.Delete` (não como parâmetro solto da Facade); decide o que fazer com transações vinculadas à entidade excluída. É passado de fora (a UI decide a política, não o contexto).
* **`TransactionScope`** (sealed: `Single`/`Future`) — viaja dentro de `TransactionCommand.Update`/`Delete`; substitui o antigo par de Strings mágicas `editMode`/`mode` (`"FUTURE"` vs. qualquer outra coisa). O parse da String de HTTP para `TransactionScope` acontece na borda da feature (`TransactionMapper.toScope`), não aqui.
* **Cartão (`CreditCard`)** é entidade própria do contexto (`last4`, `accountId`, `active`) desde a migração cartão-como-entidade; limite de crédito/cheque especial e ciclo de fatura continuam sendo colunas de `Account` (compartilhadas por todos os cartões da conta) — ver `@docs/backend/persistence-jdbc.md`.
* **`Balance`** — projeção de saldo por competência (`?period=yyyyMM`/`?year=yyyy`) exposta por `AccountUseCase` (`getMonthlyBalance`/`getYearBalances`). O record referencia o `Account` inteiro (`account`, não um `accountId` solto) e o valor fica em `value`; a chave de negócio segue sendo o par `(account.id(), period)` — sem `id` próprio; `BalanceRepository.delete(UUID, YearMonth)` (não `deleteById`) é o caminho real de exclusão usado por `BalanceService`.
* O contexto **não valida política de usuário** (ex.: período de fechamento) — isso é fronteira da feature: `f000.ClosingService.validateDate(...)`, chamado por `br.cdb.feature.f006._1_application.TransactionUseCase` (create/update/delete/transfer) em `br-application`. O contexto aceita qualquer transação bem-formada.

## Testes

`src/test/java/br/cdb/context/monetary/_1_application/usecase/` — cobre os 4 use cases (`AccountUseCaseTest`, `CreditCardUseCaseTest`, `CostCenterUseCaseTest`, `TransactionUseCaseTest`) com JUnit 5 puro, sem Quarkus/CDI/Mockito. Os fakes ficam **um pacote acima**, em `br.cdb.context.monetary`: `InMemoryRepositories` (classes aninhadas `Accounts`/`Balances`/`Cards`/`Transactions`/`CostCenters`, uma por porta) e a base `AbstractUseCaseTest`.

* **Sem construtor a injetar** — como `Service`/`UseCase` se auto-conectam via `Registry` (não recebem dependências por construtor), o setup do teste não monta objetos com `new X(fakeRepo)`; ele publica os fakes no `Registry` e deixa o `new AccountUseCase()` (sem args) resolver sozinho. Cada teste só faz `useCase = new AccountUseCase()` no próprio `@BeforeEach`.
* **`AbstractUseCaseTest` faz a limpeza global** — o `Registry` e o `MessageBus` são estáticos e persistem entre métodos, então o `@BeforeEach` da base: (1) `MessageBus.reset()`; (2) `Registry.remove(...)` dos 5 `*Service`, forçando reconstrução contra os fakes da rodada; (3) `Registry.tryGet(porta, InMemoryRepositories.X::new).clearCache()` nas 5 portas. Sem (2), o `tryGet` de um teste anterior devolveria um `Service` preso ao fake de outra rodada (ou a um repositório JDBC real, se um `@QuarkusTest` rodou antes no mesmo fork). **Estenda `AbstractUseCaseTest`** ao criar teste novo — não replique o setup.
  ⚠️ O `clearCache()` precisa rodar por método (`@BeforeEach`), nunca `@BeforeAll`, e o fake de cada porta precisa implementá-lo de verdade — um `clearCache()` vazio faz o estado vazar entre testes de forma silenciosa.
* **Não passa pela `MonetaryUseCases`** — os testes constroem o use case sob teste diretamente, sem os acessores estáticos da Facade.
* **Fakes duplicados** — existe um `InMemoryRepositories` homônimo em `br-application/src/test/java/br/cdb/context/monetary/`, para os testes da borda. Mudança de contrato de porta precisa ser aplicada nos dois.

## O que NÃO está aqui

* **Adaptadores de persistência** — vivem em `br-application`, não neste módulo; o contexto só conhece as portas (`_0_domain.repository`). Em `br.cdb.infra.persistence.monetary`: `AccountJDBCRepository` (+ `AccountTypeMapper`), `CreditCardJDBCRepository`, `CostCenterJDBCRepository`, `TransactionJDBCRepository`. A porta `BalanceRepository` é a exceção — implementada por `br.cdb.infra.persistence.features.UserAccountBalanceJDBCRepository` (tabela `PERSON_ACCOUNT_BALANCE`, do lado de feature).
* **ArchUnit e integração HTTP** — rodam em `br-application/src/test/java` (`br/cdb/ArchitectureTest.java` e `br/cdb/feature/**`; precisam de CDI/Quarkus ou do classpath completo da borda HTTP) — ver `@br-application/src/main/java/CLAUDE.md`. O que existe em `br-application/src/test/java/br/cdb/context/monetary/` são os testes de **parser/importação** (`f007`) e os fakes in-memory, não testes deste contexto.
* **Importação de extrato/PDF (parsers BTG/Santander, casamento de cartão, sugestão de categoria)** — é lógica de feature (`br.cdb.feature.f007`), não deste contexto. O contexto **não tem** conceito de importação: a feature monta o `Transaction` (domain model) e chama `TransactionUseCase.create(Transaction)` — CRUD puro (valida cartão + persiste + emite evento).

## Convenções

Mesmas do backend em geral: `@NullMarked` obrigatório (exceto enum), `val` em locais finais, `record` para modelos/commands, `Result<T, BusinessError>` (de `br-commons`) em toda operação que possa falhar — ver `@docs/backend/result-pattern.md`, `@docs/backend/null-safety.md`, `@docs/backend/lombok.md`.

## Dependências (pom)

Depende só de `br.cdb:commons`. Não depende de `context-people` nem de `br-application` — a relação de dependência é sempre de fora (`br-application`) para dentro.
