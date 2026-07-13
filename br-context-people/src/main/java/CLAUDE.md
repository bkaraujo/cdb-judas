# br-context-people — Contexto de Pessoas

Módulo Maven `context-people` (`br.cdb.contexts:context-people`, artifactId `context-people`). Contexto de negócio hexagonal, **livre de framework** — sem Quarkus/CDI/Spring, depende só de `br-commons`. É o menor e mais simples dos dois contextos.

> Índice operacional deste módulo. Visão geral da arquitetura híbrida VSA+Hexagonal em `@CLAUDE.md` (raiz) e `@docs/backend/hexagonal-architecture.md`.

## Estrutura

```
br.cdb.context.people
├── _0_domain
│   ├── model/Person.java
│   └── repository/PersonRepository.java   (porta — extends br.commons.framework.persistence.json.Repository<Person, UUID>)
├── _1_application
│   └── service/PersonService.java
├── PeopleContext.java       (Facade — único ponto de entrada externo)
└── PeopleBootstrap.java     (composition root)
```

* **`_0_domain`** — `Person` (id, name, locale, language, createdAt/updatedAt anuláveis). `PersonRepository` estende o CRUD genérico `Repository<T, ID>` de `br-commons` (pacote `persistence.json`, mas o nome é histórico — a interface é agnóstica de tecnologia; ver nota abaixo).
* **`_1_application`** — `PersonService`: `save`/`findById`, delegação direta ao repositório. Não há `UseCase` neste contexto — a Facade fala direto com o Service porque não existe orquestração multi-serviço aqui.
* **`PeopleContext`** — `implements br.commons.annotation.Facade`; expõe `registerPerson(Person)` (retorna `Person`, sem `Result`) e `findPerson(UUID)` (retorna `Optional<Person>`). Note que este contexto **não** usa o padrão `Result<T, BusinessError>` — é simples o bastante para não precisar.
* **`PeopleBootstrap`** — `register()` monta `PersonService` a partir do `PersonRepository` publicado no `Registry` e publica `PeopleContext`. Chamado por `br.cdb.core.ContextBridge.peopleContext(...)` no startup do Quarkus.

## ⚠️ Estado atual: contexto não consumido

`PeopleContext` é produzido como bean CDI (`ContextBridge.peopleContext`), mas **nenhuma feature em `br-application` o injeta ou chama** — `registerPerson`/`findPerson` não são acionados em runtime hoje. Não confunda `Person` com o agregado de login:

* **`User`** (login/sessão: `id`, `username`, `password`, `active`) vive em `br.cdb.core.web.security.User` (plataforma, `br-application`), gerido por `UserService`/`UserJDBCRepository` — é o agregado realmente ativo no fluxo de autenticação.
* **`Preferences`** (theme/language/locale/sidebarCollapsed) vive em `br.cdb.feature.user.profile.Preferences` (feature `br-application`), não neste contexto.

`Person` foi modelado como "dono de recursos" (contas etc., ver javadoc da classe) mas essa ligação ainda não foi implementada em nenhuma feature. Antes de estender este contexto, confirme com o time se a intenção é retomá-lo ou se `User` deve absorver esses campos.

## O que NÃO está aqui

* **Adaptador de persistência** — `PersonJDBCRepository` (implementação JDBC da porta, tabela `PEP_PERSON`) vive em `br-application` (`br.cdb.infra.persistence.person`), não neste módulo. O contexto só conhece a porta.
* **Testes** — este módulo não tem `src/test`. Toda a suíte de backend (unit, ArchUnit, integração HTTP) roda em `br-application/src/test/java` — ver `@br-application/src/main/java/CLAUDE.md`. Hoje não há teste dedicado a `PersonService`/`PeopleContext`.

## Convenções

Mesmas do backend em geral: `@NullMarked` obrigatório (exceto enum), `val` em locais finais, `record` para o modelo de domínio. Ver `@docs/backend/null-safety.md` e `@docs/backend/lombok.md`.

## Dependências (pom)

Depende só de `br.cdb:commons`. Não depende de `context-monetary` nem de `br-application` — a relação de dependência é sempre de fora (`br-application`) para dentro.
