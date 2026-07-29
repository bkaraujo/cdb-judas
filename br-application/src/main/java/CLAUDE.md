# Diretrizes de Desenvolvimento - CDB Finance

Este documento descreve os comandos úteis e as diretrizes arquiteturais para o desenvolvimento no backend do **CDB Finance** (Java 25 + Quarkus). É o módulo Maven `application` (`br.cdb:application`) — a borda HTTP/CDI da aplicação, empacotada como fast-jar Quarkus. Depende só de `br-commons` como módulo Maven separado; os antigos contextos `br-context-monetary`/`br-context-people` foram dissolvidos na fase 2 de `.claude/plan.md` — suas classes viraram subpacotes `fNNN._0_domain.{model,repository,event}`/`fNNN._1_application.{command,service,usecase,event}`, Registry-wired como antes.

> A documentação detalhada de cada diretriz vive em `@docs/backend/`. Este arquivo é o índice operacional; consulte os documentos referenciados antes de implementar. Convenções do framework comum ficam em `@br-commons/src/main/java/CLAUDE.md`.

## Estrutura do módulo

* **`br.cdb.core`** — plataforma: autenticação/autorização (token opaco rotativo, `OwnershipFilter`/interceptor), observabilidade (log de requisição + MDC), erro HTTP (`web/error`), `ContextBridge` (produz `DataSource` + roda as migrações one-shot + publica os adaptadores JDBC nas portas de repositório Registry-wired no `StartupEvent`; produz `UserRepository`/`PersonRepository` como beans CDI).
* **`br.cdb.feature.fNNN`** (hoje `f000`–`f006`, `f009`, `f999`) — cada feature é um hexágono auto-contido: `_0_domain` (modelos/overlays + portas `*Repository` + eventos de domínio da feature), `_1_application` (`*Service`/`*UseCase` + commands/outcomes + `@MessageListener` best-effort), `_2_infrastructure` (`*Resource`, DTOs HTTP, `*JDBCRepository`, `FNNNModule` CDI). `f000` é a fatia-base (SSE, deletion, auth, `UserGuards`, `UserService`, `InternalApi`, costcenter, version, closing, `TransactionPolicy`; estrutura plana `_0/_1/_2`, sem sub-pacotes por assunto — exceto os subpacotes remanescentes da dissolução) — todas as demais podem depender dela, ela de nenhuma feature; `f999` é o composition root (todo adapter que liga porta de uma fatia a provedor de outra vive em `f999._2_infrastructure.adapter`) — pode depender de todas. Algumas fatias (`f003`, `f009`) não tinham `_0_domain`/módulo CDI próprio antes da dissolução — `f003` ganhou um na fase 2 (hospeda `CreditCard`/`CreditCardUseCase` etc., ex-contexto monetário). `f007` (importação de extrato/fatura) fundiu-se em `f006` na fase 6 — path preservado, só o pacote/número sumiu. **Inventário de endpoints por fatia**: `br/cdb/feature/package-info.java` (fica ao lado do código; é a fonte mais precisa). Decomposição funcional em `@CLAUDE.md` (raiz); histórico da migração fatias-planas→`fNNN` em `.claude/refactor.md` (**não versionado** — `.claude/` está no `.gitignore`, só existe na máquina local).
* **`br.cdb.infra`** — `persistence` (adaptadores `*JDBCRepository` dos ex-contextos monetário/people, migrações one-shot incl. `ContextMergeMigration` — a fusão de schema da fase 2, `Database` = schema) e `message`. Adaptadores de **feature** vivem no próprio `fNNN/_2_infrastructure`, não aqui.

---
## 📐 Diretrizes de Arquitetura e Estilo

O backend segue um modelo híbrido combinando **Vertical Slice Architecture (VSA)** e **Arquitetura Hexagonal**.

### 1. Vertical Slice Architecture (VSA)
* As funcionalidades orientadas à interface/exposição HTTP são divididas em fatias verticais numeradas sob `br.cdb.feature.fNNN` (ex.: `f002` = accounts, `f003` = cards, `f006` = transactions/transfer, `f004` = tags).
* Os `*Resource` de cada fatia (em `_2_infrastructure`) **não orquestram**: delegam ao `*UseCase` da própria fatia (em `_1_application`) e fazem só tradução de formato — parse de path/query params, request → `Command`, view → DTO, `Result.Failure` → `BusinessException`/`ProblemDetail`. O `*UseCase` orquestra as engines Registry-wired ex-contexto (`fNNN._1_application.usecase.*` — `AccountUseCase`/`CreditCardUseCase`/`TransactionUseCase`/`CostCenterUseCase`, obtidas via `Registry.tryGet(XUseCase.class)`) e os serviços/portas da própria fatia (overlay, SSE via `f000`). É proibido que as classes HTTP Resource/Service de uma Feature acessem repositórios diretamente, ou classes de `_1_application`/`_2_infrastructure` de uma fatia **irmã** de negócio — cross-feature é sempre via um destes três mecanismos, nessa ordem: evento (`br.commons.MessageBus`, best-effort, record em `f000._0_domain.event`), porta declarada pelo consumidor no seu próprio `_0_domain` (retorno síncrono), ou adapter em `f999._2_infrastructure.adapter` (único lugar que conhece os dois lados). Alvo `f000` e origem `f999` são as duas exceções nomeadas na regra ArchUnit (`feature_slices_must_not_depend_on_sibling_slices`) — mais a exceção temporária fase 2→4 para remanescentes dos contextos dissolvidos (ver regra 6, abaixo). **Políticas de usuário** (ex.: período de fechamento) são validadas na **fronteira da feature** (`ClosingService.validateDate(...)`, em `f000`, consumido por `f006`), não dentro da engine — ela aceita qualquer transação bem-formada.

### 2. Arquitetura Hexagonal + DI por Registry (ex-contextos, hoje subpacotes dentro de fNNN)
Até a fase 2 de `.claude/plan.md`, cada contexto de negócio era um módulo Maven próprio (`br-context-monetary`/`br-context-people`), isolado e livre de framework. A dissolução moveu essas classes para dentro da fatia dona de cada assunto, preservando a estrutura interna que já tinham como contexto — `fNNN._0_domain.{model,repository,event}` e `fNNN._1_application.{command,service,usecase,event}` (ex.: `Account`/`AccountRepository`/`AccountUseCase` agora em `f002`; `CreditCard*` em `f003`; `Transaction*` em `f006`; `CostCenter*`/`Person*`/`TransactionPolicy` em `f000`). O que **não** mudou: essas classes continuam **livres de framework**, montando-se sozinhas via `br.commons.Registry` (service locator manual, campos `Registry.tryGet(XClass.class)`/`Registry.get(XRepository.class)` — nunca `@Inject`). A camada `feature/*` (CDI/Quarkus) alcança essas engines via `Registry.tryGet(XUseCase.class)` direto (sem Facade — `MonetaryUseCases`/`PeopleContext`/`PeopleBootstrap` morreram na dissolução). `br.cdb.core.ContextBridge` continua a única costura CDI↔Registry: publica os adaptadores `*JDBCRepository` nas portas no `StartupEvent`, antes do primeiro acesso.

**Persistência (JDBC/H2):** as portas de repositório (agora em `fNNN._0_domain.repository`) são implementadas por adaptadores `*JDBCRepository` **neste módulo**, em `br.cdb.infra.persistence`, sobre o `DataSource` H2 (dev: file `jdbc:h2:file:./database`; teste: in-memory) configurado por `DataSourceProperties` + `application.properties` e publicado no `Registry`. O schema vive em `Database` (tabelas por-fatia `FNNN_*` + lookups globais `SYS_*`; `preferences` como mapa livre em coluna JSON; as FKs conformam aos diagramas Mermaid — só lookups têm FK real, tabelas de dados se referenciam sem FK, integridade via evento — então a ordem em `model()`/`reset()` respeita a dependência pai→filho só por organização). Cartão (`F003_CARD`) é tabela própria da fatia `f003`; limite de crédito/cheque especial e ciclo de fatura são colunas da própria `F002_ACCOUNT` (compartilhadas por todos os cartões da conta, que também carrega `COD_PERSON`/`TXT_COLOR` desde a fusão com o antigo overlay `PERSON_ACCOUNT`). As tabelas de dados fazem chave com `F000_PERSON` (`COD_PERSON`); `F000_USER`/`F000_USER_CREDENTIAL` servem só ao login (identificam a pessoa). Os primitivos JDBC ficam em `br.commons.framework.persistence.jdbc`.

**A persistência é 100% JDBC — não há mais nenhum agregado em JSON.** `Closing` grava na tabela `F000_PREFERENCES` (`ClosingJDBCRepository`, em `f000/_2_infrastructure/persistence`) e o catálogo de centros de custo é a tabela `F000_COST_CENTER`, semeada no próprio DDL de `Database`. O stack JSON de `br.commons` sobrevive só como resíduo: `br.cdb.core.persistence.JsonStorageConfig` ainda produz um bean `Storage` (`LocalFileStorage`) e `JsonStorageProperties`/`STORAGE_JSON_PATH` ainda existem, **sem nenhum consumidor** — `JsonStorageConfig` continua necessário pelo outro papel, o de `ObjectMapperCustomizer` (BigDecimal com 2 casas, enums de transação em lowercase). Todos os adaptadores JDBC (ex-contexto e feature) vivem em `br.cdb.infra.persistence` ou no `fNNN/_2_infrastructure/persistence` da própria fatia, conforme onde a porta foi parar na dissolução.

**Transações** são ambiente (por thread) com propagação **REQUIRED**: dentro de um `dataSource.transaction(...)`, toda operação aninhada — `query`/`execute`, `save` de repositório, introspecção de um `*JDBCRepository` criado lazy — participa da transação em curso; só o nível mais externo commita e devolve a conexão ao pool. Consequência direta: como o `MessageBus` despacha **sincronamente e na mesma thread**, um `@MessageListener` que escreve no banco (ex.: o seed de categorias de `f005` reagindo a `UserEvents.Created`) commita junto com quem publicou o evento — e a falha dele reverte o trabalho do publicador. É o que faz `f999` criar cada usuário (Person + login + credencial + categorias) numa única transação.

Detalhes em **`@docs/backend/persistence-jdbc.md`** (schema = diagramas Mermaid; ciclo de vida do DB de dev file-based; leitura de colunas anuláveis; migrações one-shot: cartão legado, merge de limite, schema de features, fusão dos contextos; §6 propagação de transação) — alguns exemplos desse documento ainda podem citar nomes de tabela pré-fase-1 (`MON_*`/`PEP_*`/`PERSON_*`), atualização pendente para a fase 6.

O fluxo de dependência `Resource (fNNN/_2) → UseCase (fNNN/_1) → engine ex-contexto (fNNN._1_application.usecase, via Registry) → Service → Repository (porta, fNNN/_0) → *JDBCRepository (adaptador, fNNN/_2)` e a responsabilidade de cada camada estão detalhados em **`@docs/backend/hexagonal-architecture.md`** (mesma ressalva de nomenclatura pré-fase-2 acima).

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
  5. `feature_slices_depend_only_on_earlier_ones`: O número da fatia expressa ordem de criação — historicamente uma `fNNN` só consumia recursos de fatias com número menor. Continua na suíte como **rede de segurança barata** (`ArchCondition` custom que extrai o número do pacote via regex e verifica `alvo ≤ origem`), mas não é mais o mecanismo de desacoplamento: com a regra 6 abaixo, o número virou só agrupamento legível, e renumerar uma fatia (`f003`→`f004`, etc.) é seguro.
  6. `feature_slices_must_not_depend_on_sibling_slices`: **fatia de negócio não importa fatia de negócio irmã**, em nenhuma direção. Duas exceções nomeadas, por papel arquitetural (não por número): alvo `f000` (kernel compartilhado) sempre permitido; origem `f999` (composition root) sempre permitida. Os casos em que uma fatia precisa de serviço de outra resolvem-se por: **evento** em `f000._0_domain.event` para efeito sem retorno (best-effort, ex.: `AccountStreamEvents`, `TransactionsDeleted`, `CategoryReassigned`, `TransactionImported`); **`f000.InternalApi`** para leitura síncrona que precisa de retorno (HTTP real contra o endpoint público da fatia dona, token efêmero de `AccessTokenStore` — nunca o token de sessão do navegador, ver javadoc da classe; ex.: `f005.CategoryUseCase`→`GET /accounts/transactions/by-category` de f006, `f006.TransactionUseCase`→`GET /categories/transfer` de f005, `f009.DashboardService`→`GET /accounts/transactions` de f006); ou **adapter em `f999._2_infrastructure.adapter`** implementando uma porta declarada pelo consumidor no seu `_0_domain` (CDI puro, sem `@Produces`/`Registry`) — a fase 4 matou os 4 casos que existiam até a fase 3 (`TransferCategoriesAdapter`, `TransactionOverlaySinkAdapter`, `TransactionAccountOverlayAdapter`, `TransactionCategoryOverlayAdapter`); `DeletionQueueAdapter` (fase 5, liga `f002.DeletionQueue` a `f999.DeletionQueueService`) é a instância viva de hoje — ver `.claude/refactor.md`. **Exceção temporária (desde a fase 2 de `.claude/plan.md`):** alvo remanescente dos contextos recém-dissolvidos (subpacotes `_0_domain.model/repository/event`, `_1_application.command/service/usecase/event` dentro de `fNNN` — a organização interna que a classe já tinha como contexto) também é tolerado, de qualquer origem. A fase 6 fechou os dois casos nomeados até então — `f007` (funde em `f006`, deixa de ser cross-slice) e `f009` (troca para `InternalApi`) — mas resta `UserGuards` (em `f000`, chama `f002.AccountUseCase`/`f003.CreditCardUseCase` direto), sem fase nomeada pra fechar ainda. Ver javadoc de `isDissolvedContextRemnant` em `ArchitectureTest`.

  As antigas regras `user_feature_slices_must_not_depend_on_each_other` (matcher `..feature.user.(*)..`) e `auth_feature_must_not_access_user_features` foram removidas na migração fNNN (`.claude/refactor.md`), que esvaziou `feature.user.*` (matchers casavam zero classes → ArchUnit falha em "empty should"). As regras `feature_must_access_context_only_via_facade_or_domain_model` e `context_must_not_depend_on_framework` saíram na fase 2 da dissolução dos contextos (`.claude/plan.md`) — não fazem mais sentido sem contexto separado para acoplar-se ou para ser framework-free. A regra 6 acima é a substituição endurecida e definitiva: nenhuma exceção "para baixo" restou, tudo cross-slice passa por evento/`InternalApi`/adapter (com a exceção temporária documentada acima pro resíduo de acesso direto à engine que nenhuma fase fechou ainda).

---

## 🏗️ Qualidade & Build

Gate de qualidade (PMD/CPD *enforcing* no `verify`), gotchas de build do Java 25, verificação de backend sem `mvn` e setup de run/debug na IDE em **`@docs/backend/quality-and-build.md`**.
