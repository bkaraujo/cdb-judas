# Diretrizes de Desenvolvimento - CDB Finance

Este documento descreve os comandos úteis e as diretrizes arquiteturais para o desenvolvimento no backend do **CDB Finance** (Java 25 + Spring Boot 4).

> A documentação detalhada de cada diretriz vive em `@docs/backend/`. Este arquivo é o índice operacional; consulte os documentos referenciados antes de implementar.

---
## 📐 Diretrizes de Arquitetura e Estilo

O backend segue um modelo híbrido combinando **Vertical Slice Architecture (VSA)** e **Arquitetura Hexagonal**.

### 1. Vertical Slice Architecture (VSA)
* As funcionalidades orientadas à interface/exposição HTTP são divididas em fatias verticais sob o pacote `br.community.feature.<feature_name>` (ex: `dashboard`, `records/accounts`, `operations/transactions`).
* A comunicação de uma Feature com o restante do sistema ocorre exclusivamente por meio das **Facades** dos contextos de negócio (ex: `MonetaryContext`). É proibido que as classes HTTP Resource/Service de uma Feature acessem repositórios ou lógica interna de contextos diretamente.

### 2. Arquitetura Hexagonal (Dentro de Contextos)
Cada contexto de negócio (ex: `br.community.context.monetary`) é isolado internamente e estruturado em camadas concêntricas:
* **`_0_domain` (Domínio):** Contém os modelos (`model`), contratos de repositório (`repository`) e portas (`port`). Não deve possuir dependência de tecnologias externas, frameworks ou detalhes de infraestrutura.
* **`_1_application` (Aplicação):** Contém commands (`command`) para transporte de dados de entrada, escutadores de eventos (`event`) e lógica de negócio pura (`service`).
* **`_2_infrastructure` (Infraestrutura):** Contém implementações concretas de portas de persistência (armazenamento JDBC ou em memória).
* **`UseCase`:** Classes de orquestração de alto nível que expõem os serviços do contexto recebendo Commands.

O fluxo de dependência `Resource → UseCase → Service → Repository` e a responsabilidade de cada camada estão detalhados em **`@docs/backend/hexagonal-architecture.md`**.

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
  1. `resources_must_not_access_repositories`: Controladores HTTP (`Resource`) não podem injetar/acessar repositórios diretamente.
  2. `all_classes_must_be_null_marked`: Garante a anotação `@NullMarked` obrigatória.
  3. `core_must_not_access_feature`: O núcleo comum não pode depender de fatias de features de entrega.
  4. `application_must_not_access_infrastructure`: Classes de aplicação (`_1_application`) não podem depender diretamente da infraestrutura (`_2_infrastructure`).
  5. `feature_must_access_context_only_via_facade_or_domain_model`: Garante o acoplamento correto entre a camada web e os contextos apenas via Facade e modelos expostos.
