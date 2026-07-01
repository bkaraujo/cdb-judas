# Diretrizes de Desenvolvimento - CDB Finance

Este documento descreve os comandos úteis e as diretrizes arquiteturais para o desenvolvimento no backend do **CDB Finance** (Java 25 + Quarkus).

> A documentação detalhada de cada diretriz vive em `@docs/backend/`. Este arquivo é o índice operacional; consulte os documentos referenciados antes de implementar.

---
## 📐 Diretrizes de Arquitetura e Estilo

O backend segue um modelo híbrido combinando **Vertical Slice Architecture (VSA)** e **Arquitetura Hexagonal**.

### 1. Vertical Slice Architecture (VSA)
* As funcionalidades orientadas à interface/exposição HTTP são divididas em fatias verticais sob o pacote `br.community.feature.<feature_name>` (ex: `dashboard`, `records/accounts`, `operations/transactions`).
* A comunicação de uma Feature com o restante do sistema ocorre exclusivamente por meio das **Facades** dos contextos de negócio (ex: `MonetaryContext`). É proibido que as classes HTTP Resource/Service de uma Feature acessem repositórios ou lógica interna de contextos diretamente. **Políticas de usuário** (ex.: período de fechamento) são validadas na **fronteira da feature** (Resource), não dentro do contexto — o contexto aceita qualquer transação bem-formada (ex.: `TransactionResource`/`TransferResource` chamam `ClosingService.validateDate(...)` antes de delegar à Facade).

### 2. Arquitetura Hexagonal + DI por Registry (Dentro de Contextos)
Cada contexto de negócio (`br.community.context.monetary`, `...people`) é isolado e **livre de framework**:
* **`_0_domain` (Domínio):** modelos (`model`), eventos (`event`) e contratos de repositório/portas (`repository`). Sem dependência de frameworks ou infraestrutura.
* **`_1_application` (Aplicação):** commands (`command`), escutadores de eventos (`event`), serviços (`service`) e use-cases (`usecase`) de orquestração.
* **Injeção de dependência (modelo híbrido):** o contexto é montado por um *composition root* (`MonetaryBootstrap`/`PeopleBootstrap`) via `br.commons.Registry` (service locator manual) — **sem framework no contexto** (garantido pelas regras ArchUnit `context_must_not_depend_on_spring` e `no_class_depends_on_spring`). A camada `feature/*` usa CDI (Quarkus/Arc) e alcança o contexto **apenas via Facade**, reexposta como bean por `br.community.core.ContextBridge` (única costura CDI↔Registry).

**Persistência (JDBC/H2):** as portas de repositório são implementadas por adaptadores `*JDBCRepository` em `br.community.infra.persistence`, sobre o `DataSource` H2 (dev: file `jdbc:h2:file:./database`; teste: in-memory) configurado por `DataSourceProperties` + `application.properties` e publicado no `Registry`. O schema vive em `Database` (tabelas planas; enums como string referenciando tabelas de domínio/lookup `MON_ACCOUNT_TYPE`/`MON_NATURE`/`MON_TRANSACTION_STATUS` via FK; mapas livres como `additionalInfo`/`preferences` em coluna JSON; sem outras chaves estrangeiras entre tabelas de dados). Os primitivos JDBC ficam em `br.commons.framework.persistence.jdbc`. (A feature `Closing` ainda usa JSON via `Storage`/`LocalFileStorage`, assim como o catálogo global de centros de custo.) Detalhes em **`@docs/backend/persistence-jdbc.md`** (schema = diagramas Mermaid; ciclo de vida do DB de dev file-based; leitura de colunas anuláveis).

O fluxo de dependência `Resource → Facade → UseCase → Service → Repository (porta) → *JDBCRepository (adaptador)` e a responsabilidade de cada camada estão detalhados em **`@docs/backend/hexagonal-architecture.md`**.

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
* **Testes de Arquitetura (ArchUnit):** O arquivo `br.community.ArchitectureTest` automatiza a validação das regras arquiteturais no pipeline. As regras principais validadas são:
  1. `resources_must_not_access_repositories`: Controladores HTTP (`Resource`) não podem injetar/acessar repositórios diretamente (exceção nomeada: `LoginResource`, que acessa `UserRepository` direto para autenticação).
  2. `all_classes_must_be_null_marked`: Garante a anotação `@NullMarked` obrigatória.
  3. `core_must_not_access_feature`: O núcleo comum não pode depender de fatias de features de entrega.
  4. `application_must_not_access_infrastructure`: Classes de aplicação (`_1_application`) não podem depender diretamente da infraestrutura (`_2_infrastructure`).
  5. `feature_must_access_context_only_via_facade_or_domain_model`: Garante o acoplamento correto entre a camada web e os contextos apenas via Facade e modelos expostos.
  6. `context_must_not_depend_on_spring`: Garante que o contexto é livre de framework (DI via `Registry`).
  7. `no_class_depends_on_spring`: Nenhuma classe do app (não só o contexto) depende de Spring — checagem de que a migração para Quarkus não deixou resíduo.

---

## 🏗️ Qualidade & Build

Gate de qualidade (PMD/CPD *enforcing* no `verify`), gotchas de build do Java 25, verificação de backend sem `mvn` e setup de run/debug na IDE em **`@docs/backend/quality-and-build.md`**.
