# Diretrizes de Desenvolvimento - CDB Finance

Este documento descreve os comandos úteis e as diretrizes arquiteturais para o desenvolvimento no backend do **CDB Finance** (Java 25 + Quarkus). É o módulo Maven `application` (`br.cdb:application`) — a borda HTTP/CDI da aplicação, empacotada como fast-jar Quarkus. Depende de `br-context-monetary`, `br-context-people` e `br-commons` como módulos Maven separados (não sub-pacotes deste).

> A documentação detalhada de cada diretriz vive em `@docs/backend/`. Este arquivo é o índice operacional; consulte os documentos referenciados antes de implementar. Convenções internas de cada contexto/framework ficam nos respectivos `@br-context-monetary/src/main/java/CLAUDE.md`, `@br-context-people/src/main/java/CLAUDE.md` e `@br-commons/src/main/java/CLAUDE.md`.

## Estrutura do módulo

* **`br.cdb.core`** — plataforma: autenticação/autorização (token opaco rotativo, `OwnershipFilter`/interceptor), observabilidade (log de requisição + MDC), erro HTTP (`web/error`), `ContextBridge` (costura CDI↔`Registry`: produz `DataSource`, registra os adaptadores JDBC nas portas dos contextos no `StartupEvent` e produz `UserRepository`/`PersonRepository`/`PeopleContext` como beans).
* **`br.cdb.feature.fNNN`** (hoje `f000`–`f007`, `f009`, `f999`) — cada feature é um hexágono auto-contido: `_0_domain` (modelos/overlays + portas `*Repository` + eventos de domínio da feature), `_1_application` (`*Service`/`*UseCase` + commands/outcomes + `@MessageListener` best-effort), `_2_infrastructure` (`*Resource`, DTOs HTTP, `*JDBCRepository`, `FNNNModule` CDI — algumas fatias finas, como `f003`/`f009`, não têm `_0_domain` nem módulo CDI próprio). `f000` é a fatia-base (SSE, deletion, auth, `UserGuards`, `UserService`, costcenter, version, closing; estrutura plana `_0/_1/_2`, sem sub-pacotes por assunto) — todas as demais podem depender dela, ela de nenhuma feature; `f999` é o composition root (todo adapter que liga porta de uma fatia a provedor de outra vive em `f999._2_infrastructure.adapter`) — pode depender de todas. **Inventário de endpoints por fatia**: `br/cdb/feature/package-info.java` (fica ao lado do código; é a fonte mais precisa). Decomposição funcional em `@CLAUDE.md` (raiz); histórico da migração fatias-planas→`fNNN` em `.claude/refactor.md` (**não versionado** — `.claude/` está no `.gitignore`, só existe na máquina local).
* **`br.cdb.infra`** — `persistence` (adaptadores `*JDBCRepository` das **portas de contexto**, migrações one-shot, `Database` = schema) e `message`. Adaptadores de **feature** vivem no próprio `fNNN/_2_infrastructure`, não aqui.

---
## 📐 Diretrizes de Arquitetura e Estilo

O backend segue um modelo híbrido combinando **Vertical Slice Architecture (VSA)** e **Arquitetura Hexagonal**.

### 1. Vertical Slice Architecture (VSA)
* As funcionalidades orientadas à interface/exposição HTTP são divididas em fatias verticais numeradas sob `br.cdb.feature.fNNN` (ex.: `f002` = accounts, `f003` = cards, `f006` = transactions/transfer, `f004` = tags).
* Os `*Resource` de cada fatia (em `_2_infrastructure`) **não orquestram**: delegam ao `*UseCase` da própria fatia (em `_1_application`) e fazem só tradução de formato — parse de path/query params, request → `Command`, view → DTO, `Result.Failure` → `BusinessException`/`ProblemDetail`. O `*UseCase` orquestra os use cases do contexto (obtidos pelos acessores estáticos da Facade `MonetaryUseCases`: `ucAccount()`/`ucCreditCard()`/`ucTransaction()`/`ucCostCenter()`) e os serviços/portas da própria fatia (overlay, SSE via `f000`). É proibido que as classes HTTP Resource/Service de uma Feature acessem repositórios ou lógica interna de contextos diretamente, ou classes de `_1_application`/`_2_infrastructure` de uma fatia **irmã** de negócio — cross-feature é sempre via um destes três mecanismos, nessa ordem: evento (`br.commons.MessageBus`, best-effort, record em `f000._0_domain.event`), porta declarada pelo consumidor no seu próprio `_0_domain` (retorno síncrono), ou adapter em `f999._2_infrastructure.adapter` (único lugar que conhece os dois lados). Alvo `f000` e origem `f999` são as duas exceções nomeadas na regra ArchUnit (`feature_slices_must_not_depend_on_sibling_slices`). **Políticas de usuário** (ex.: período de fechamento) são validadas na **fronteira da feature** (`ClosingService.validateDate(...)`, em `f000`, consumido por `f006`), não dentro do contexto — o contexto aceita qualquer transação bem-formada.

### 2. Arquitetura Hexagonal + DI por Registry (contextos são módulos Maven à parte)
Cada contexto de negócio é um **módulo Maven próprio** — `br-context-monetary` (`br.cdb.context.monetary`) e `br-context-people` (`br.cdb.context.people`) — isolado e **livre de framework**, dependendo só de `br-commons`. Estrutura interna (`_0_domain`/`_1_application`, modelos, use cases, convenções específicas) documentada em `@br-context-monetary/src/main/java/CLAUDE.md` e `@br-context-people/src/main/java/CLAUDE.md`; aqui vale a regra de acoplamento que **este** módulo (`br-application`) deve respeitar:
* **Injeção de dependência (modelo híbrido):** o contexto se monta sozinho via `br.commons.Registry` (service locator manual; `people` ainda usa o composition root `PeopleBootstrap`) — **sem framework no contexto** (garantido pela regra ArchUnit `context_must_not_depend_on_framework`). A camada `feature/*` (deste módulo) usa CDI (Quarkus/Arc) e alcança `monetary` **apenas pelos use cases devolvidos pelos acessores estáticos da Facade** (`MonetaryUseCases.uc*()`). Em `people` o padrão é outro: `f000.UserService` e `f001.ProfileService` instanciam `PersonUseCase` diretamente (`new`), e o use case resolve o `PersonService` no `Registry`; a Facade `PeopleContext` é produzida como bean por `ContextBridge`, mas hoje ninguém a injeta. As duas formas passam na regra `feature_must_access_context_only_via_facade_or_domain_model`, que autoriza Facade **e** `_1_application.usecase`. `br.cdb.core.ContextBridge` (única costura CDI↔Registry) publica as portas de repositório no `Registry` no startup, antes do primeiro acesso.

**Persistência (JDBC/H2):** as portas de repositório (definidas nos módulos de contexto) são implementadas por adaptadores `*JDBCRepository` **neste módulo**, em `br.cdb.infra.persistence`, sobre o `DataSource` H2 (dev: file `jdbc:h2:file:./database`; teste: in-memory) configurado por `DataSourceProperties` + `application.properties` e publicado no `Registry`. O schema vive em `Database` (tabelas planas; enums como string referenciando tabelas de domínio/lookup `MON_ACCOUNT_TYPE`/`TRANSACTION_NATURE`/`MON_STATUS` via FK; `preferences` como mapa livre em coluna JSON; as FKs conformam a todas as relações dos diagramas Mermaid — além das lookups, as tabelas de dados referenciam-se entre si, inclusive entre contextos, então a ordem em `model()`/`reset()` respeita a dependência pai→filho). Cartão (`MON_CARD`) é tabela própria do contexto monetário; limite de crédito/cheque especial e ciclo de fatura são colunas da própria `MON_ACCOUNT` (compartilhadas por todos os cartões da conta). As tabelas de dados fazem chave com `PEP_PERSON` (`COD_PERSON`); `SEC_USER`/`USER_CREDENTIAL` servem só ao login (identificam a pessoa). `PERSON_ACCOUNT` (overlay da feature `accounts`) hoje só guarda a cor por pessoa — saldo e estado ativo vêm inteiramente do contexto monetário; saldo inicial histórico não existe mais como conceito (virou transação normal numa migração). Os primitivos JDBC ficam em `br.commons.framework.persistence.jdbc`.

**A persistência é 100% JDBC — não há mais nenhum agregado em JSON.** `Closing` grava na tabela `PERSON_PREFERENCES` (`ClosingJDBCRepository`, em `f000/_2_infrastructure/persistence`) e o catálogo de centros de custo é a tabela `MON_COST_CENTER`, semeada no próprio DDL de `Database`. O stack JSON de `br.commons` sobrevive só como resíduo: `br.cdb.core.persistence.JsonStorageConfig` ainda produz um bean `Storage` (`LocalFileStorage`) e `JsonStorageProperties`/`STORAGE_JSON_PATH` ainda existem, **sem nenhum consumidor** — `JsonStorageConfig` continua necessário pelo outro papel, o de `ObjectMapperCustomizer` (BigDecimal com 2 casas, enums de transação em lowercase). Adaptadores de **contexto** ficam em `br.cdb.infra.persistence`; adaptadores de **feature**, no `fNNN/_2_infrastructure/persistence` da própria fatia.

**Transações** são ambiente (por thread) com propagação **REQUIRED**: dentro de um `dataSource.transaction(...)`, toda operação aninhada — `query`/`execute`, `save` de repositório, introspecção de um `*JDBCRepository` criado lazy — participa da transação em curso; só o nível mais externo commita e devolve a conexão ao pool. Consequência direta: como o `MessageBus` despacha **sincronamente e na mesma thread**, um `@MessageListener` que escreve no banco (ex.: o seed de categorias de `f005` reagindo a `UserEvents.Created`) commita junto com quem publicou o evento — e a falha dele reverte o trabalho do publicador. É o que faz `f999` criar cada usuário (Person + login + credencial + categorias) numa única transação.

Detalhes em **`@docs/backend/persistence-jdbc.md`** (schema = diagramas Mermaid; ciclo de vida do DB de dev file-based; leitura de colunas anuláveis; migrações one-shot: cartão legado, merge de limite, schema de features; §6 propagação de transação).

O fluxo de dependência `Resource (fNNN/_2) → UseCase (fNNN/_1) → UseCase (contexto, via Facade) → Service → Repository (porta, fNNN/_0) → *JDBCRepository (adaptador, fNNN/_2)` e a responsabilidade de cada camada estão detalhados em **`@docs/backend/hexagonal-architecture.md`**.

### 3. Estilo
* Favoreça composição sobre herança; lógica usada 2+ vezes vira utilitário.
* Helpers de controller compartilhados vão num utilitário `final` com construtor privado e métodos `static` (ex.: `TransactionMapper`), **nunca** numa classe `abstract @RestController` base — uma superclasse abstrata torna o bean elegível a CGLIB/lookup-method e quebra com `Lookup method resolution failed`.

---

## 🚦 Tratamento de Erros — Padrão Result

O projeto evita exceções para controle de fluxo de negócio e adota o tipo `Result<T, E>` (Railway Oriented Programming). Erros de domínio são objetos de valor (`UserAlreadyExists`, `InsufficientFunds`); a tradução para HTTP/Web acontece apenas na borda (Resource). Exceções de runtime permanecem reservadas a falhas fatais de infraestrutura.

Filosofia, composição (`map`/`flatMap`/`recover`) e integração com o hexágono em **`@docs/backend/result-pattern.md`**.

---

## 🧩 Lombok

* `val` em toda variável local tratável como `final`.
* `@RequiredArgsConstructor` para injeção via construtor (`private final`).
* `@Getter`/`@Builder` quando necessário; `record` é o padrão para modelos de domínio e DTOs.
* **Evitar** `@Data`, `@Setter` e `@AllArgsConstructor` no domínio.

Detalhes e configuração de `lombok.config` em **`@docs/backend/lombok.md`**.

---

## 🛡️ Null-Safety (JSpecify + NullAway & ErrorProne)

O compilador está configurado para falhar em caso de violação de null-safety.

* **Obrigatoriedade de `@NullMarked`:** Todas as classes/interfaces no pacote `br..` devem ser anotadas com `@NullMarked` (de `org.jspecify.annotations.NullMarked`). Exceção: `enum`.
* **Uso de `@Nullable`:** Anote explicitamente parâmetros, campos ou retornos que podem ser nulos com `org.jspecify.annotations.Nullable`.
* **Lombok Getter Exclusion:** A checagem de NullAway ignora `@Getter` do Lombok para evitar falsos positivos.

Contrato completo, checklist por classe e integração com ArchUnit em **`@docs/backend/null-safety.md`**.

---

## 🧪 Estratégia de Testes

* **Testes Unitários:** Desenvolva testes de unidade sob `src/test/java` com JUnit 5 para garantir o comportamento correto das lógicas de serviços, use cases, parsers e validadores.
* **Testes de Arquitetura (ArchUnit):** O arquivo `br.cdb.ArchitectureTest` automatiza a validação das regras arquiteturais no pipeline. As regras principais validadas são:
  1. `resources_must_not_access_repositories`: Controladores HTTP (`Resource`) não podem injetar/acessar repositórios diretamente (exceções nomeadas: `LoginResource`, que acessa `UserRepository` direto para autenticação, e `SelfResource`, que o acessa direto para resolver o `username` de login).
  2. `all_classes_must_be_null_marked`: Garante a anotação `@NullMarked` obrigatória.
  3. `core_must_not_access_feature`: O núcleo comum não pode depender de fatias de features de entrega.
  4. `application_must_not_access_infrastructure`: Classes de `_1_application` não podem depender diretamente de `_2_infrastructure` — **regra viva** contra as fatias `fNNN` (cada uma tem seu próprio `_2_infrastructure` agora); DTOs/helpers consumidos por `_1` (ex.: `AccountResponse`, `Deletions`) moram em `_1_application`, não em `_2`, exatamente para respeitar esta regra.
  5. `feature_must_access_context_only_via_facade_or_domain_model`: Garante o acoplamento correto entre a camada web e os contextos — apenas via Facade, use cases (`_1_application.usecase`), commands/eventos de aplicação e modelos/eventos de domínio.
  6. `context_must_not_depend_on_framework`: O contexto é livre de framework — nem Spring (resíduo pré-Quarkus), nem `jakarta..`/`io.quarkus..` (a validação real mora nos `*Request` da borda, com `@Valid`).
  7. `feature_slices_depend_only_on_earlier_ones`: O número da fatia expressa ordem de criação — historicamente uma `fNNN` só consumia recursos de fatias com número menor. Continua na suíte como **rede de segurança barata** (`ArchCondition` custom que extrai o número do pacote via regex e verifica `alvo ≤ origem`), mas não é mais o mecanismo de desacoplamento: com a regra 8 abaixo, o número virou só agrupamento legível, e renumerar uma fatia (`f003`→`f004`, etc.) é seguro.
  8. `feature_slices_must_not_depend_on_sibling_slices`: **fatia de negócio não importa fatia de negócio irmã**, em nenhuma direção. Duas exceções nomeadas, por papel arquitetural (não por número): alvo `f000` (kernel compartilhado) sempre permitido; origem `f999` (composition root) sempre permitida. Os casos em que uma fatia precisa de serviço de outra resolvem-se por: **evento** em `f000._0_domain.event` (best-effort, ex.: `AccountStreamEvents`, `TransactionsDeleted`); **porta declarada pelo consumidor** no seu próprio `_0_domain` (ex.: `f006.TransferCategories`, `f007.TransactionOverlaySink`, `f002.TransactionAccountOverlay`, `f005.TransactionCategoryOverlay`); ou **adapter em `f999._2_infrastructure.adapter`** implementando a porta e delegando ao provedor — é o único lugar do código que conhece os dois lados (`TransferCategoriesAdapter`, `TransactionOverlaySinkAdapter`, `TransactionAccountOverlayAdapter`, `TransactionCategoryOverlayAdapter`). Resolvido por CDI puro (nenhuma porta tem `@Produces`/`Registry`: o adapter é a única implementação no classpath).

  As antigas regras `user_feature_slices_must_not_depend_on_each_other` (matcher `..feature.user.(*)..`) e `auth_feature_must_not_access_user_features` foram removidas na migração fNNN (`.claude/refactor.md`), que esvaziou `feature.user.*` (matchers casavam zero classes → ArchUnit falha em "empty should"). A regra 8 acima é a substituição endurecida e definitiva: nenhuma exceção "para baixo" restou, tudo cross-slice passa por evento/porta/adapter.

---

## 🏗️ Qualidade & Build

Gate de qualidade (PMD/CPD *enforcing* no `verify`), gotchas de build do Java 25, verificação de backend sem `mvn` e setup de run/debug na IDE em **`@docs/backend/quality-and-build.md`**.
