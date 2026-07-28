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
│   ├── service/PersonService.java
│   └── usecase/PersonUseCase.java          (ponto de entrada realmente usado pelas features)
├── PeopleContext.java       (Facade)
└── PeopleBootstrap.java     (composition root)
```

* **`_0_domain`** — `Person` (id, name, locale, language, createdAt/updatedAt anuláveis). `PersonRepository` estende o CRUD genérico `Repository<T, ID>` de `br-commons` (pacote `persistence.json`, mas o nome é histórico — a interface é agnóstica de tecnologia; o adaptador real é JDBC, ver "O que NÃO está aqui").
* **`_1_application`**
  * `PersonService`: `save`/`findById`/`rename`, delegação direta ao repositório.
  * `PersonUseCase`: resolve o `PersonService` pelo `Registry` (`Registry.tryGet`) no próprio campo, então é instanciável com `new` sem nenhum wiring externo. Expõe `register(name, locale, language)`, `findById(UUID|String)` e `rename(person, newName)`, **todos devolvendo `Result<Person, BusinessError>`** (`findById` mapeia ausência para `BusinessError.NotFound`).
* **`PeopleContext`** — `implements br.commons.annotation.Facade`; expõe `registerPerson(Person)` (retorna `Person` cru), `findPerson(UUID|String)` (`Optional<Person>`) e `renamePerson(person, newName)` (`Result`). Contrato misto — parte com `Result`, parte sem.
* **`PeopleBootstrap`** — `register()` monta `PersonService` a partir do `PersonRepository` publicado no `Registry` e publica `PeopleContext`. Chamado por `br.cdb.core.ContextBridge.peopleContext(...)` no startup do Quarkus.

## ⚠️ Estado atual: contexto consumido via `PersonUseCase`, não via Facade

O contexto **está em uso** — `Person` é hoje o dono dos recursos: todas as tabelas de dados fazem chave com `PEP_PERSON` (`COD_PERSON`), e o `{uuid}` das rotas `/api/{uuid}/…` é o `personId`. Consumidores em `br-application`:

* **`f000.UserService.createUser`** — chama `PersonUseCase.register(...)` **antes** de criar o `User`, e o `personId` resultante vira campo obrigatório do login.
* **`f001.ProfileService`** — instancia `PersonUseCase` para ler/renomear a pessoa por trás de `GET`/`PATCH /api/me`; `f001._0_domain.Profile` compõe `Person` (deste contexto) + `Preferences` (da feature).

Mas o acesso **não passa pela Facade**: ambos fazem `new PersonUseCase()` e o use case resolve o `PersonService` no `Registry`. O bean `PeopleContext` produzido por `ContextBridge.peopleContext(...)` **não é injetado por ninguém** — existe só para disparar `PeopleBootstrap.register()`. A regra ArchUnit `feature_must_access_context_only_via_facade_or_domain_model` autoriza Facade **e** `_1_application.usecase`, então isso não quebra o build; é uma divergência de estilo em relação a `monetary` (que é acessado pelos acessores estáticos de `MonetaryUseCases`). Se for unificar o padrão, mexa aqui — não na regra.

Não confunda `Person` com o agregado de login:

* **`User`** (login/sessão: `id`, `username`, `password`, `active`, `personId`) vive em `br.cdb.core.security.User` (plataforma, `br-application`), gerido por `f000.UserService`/`UserJDBCRepository`. `SEC_USER`/`USER_CREDENTIAL` servem só à autenticação — identificam a pessoa.
* **`Preferences`** (theme/language/locale/sidebarCollapsed) vive em `br.cdb.feature.f001._0_domain.Preferences` (feature `br-application`), não neste contexto.

## O que NÃO está aqui

* **Adaptador de persistência** — `PersonJDBCRepository` (implementação JDBC da porta, tabela `PEP_PERSON`) vive em `br-application` (`br.cdb.infra.persistence.person`), não neste módulo. O contexto só conhece a porta.
* **Testes** — este módulo não tem `src/test`. Toda a suíte de backend (unit, ArchUnit, integração HTTP) roda em `br-application/src/test/java` — ver `@br-application/src/main/java/CLAUDE.md`. Não há teste dedicado a `PersonService`/`PersonUseCase`/`PeopleContext`; a única cobertura indireta é `F001ProfileServiceTest`.

## Convenções

Mesmas do backend em geral: `@NullMarked` obrigatório (exceto enum), `val` em locais finais, `record` para o modelo de domínio. Ver `@docs/backend/null-safety.md` e `@docs/backend/lombok.md`.

## Dependências (pom)

Depende só de `br.cdb:commons`. Não depende de `context-monetary` nem de `br-application` — a relação de dependência é sempre de fora (`br-application`) para dentro.
