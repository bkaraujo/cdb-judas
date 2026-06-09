# Issue 01 — Read pipe: `GET /api/me` + inicial no avatar

## Parent

[`.claude/plans/perfil-configuracoes.md`](../../.claude/plans/perfil-configuracoes.md) — PRD "Perfil do Usuário & Configurações"

## What to build

Tracer bullet fundacional: caminho de **leitura** ponta a ponta do recurso `self`,
provando todas as camadas (domínio → aplicação → fachada → resource → frontend) com
largura mínima.

- **Domínio (contexto `security`):** `User` ganha campo `name` anulável (nome de
  exibição) e um record tipado `Preferences { theme (anulável), language, locale,
  sidebarCollapsed }`. `username` e `password` permanecem.
- **Dados legados:** carregamento tolerante a registros sem `name`/`preferences` —
  `preferences` ausente assume `{ theme: null, language: "pt-BR", locale: "pt-BR",
  sidebarCollapsed: false }`; `name` ausente resolve para exibição via `username`. O
  seeder do usuário admin passa a aplicar esses padrões.
- **Camada de aplicação (nova em `security/_1_application`):** `UserUseCase.getMe`
  retornando `Result<_, DomainError>` (prior art: `AccountUseCase`).
- **Fachada:** contexto de segurança expõe um tipo `implements Facade` (de
  `br.commons.annotation.Facade`) com `getMe`, registrado/wired em `SecurityModule`.
- **API:** recurso `self` com `GET /api/me` → `{ id, username, name, preferences:
  { theme, language, locale, sidebarCollapsed } }`. Identidade vem do contexto
  autenticado (sem id no caminho → sem IDOR). Sem token → `401`.
- **Frontend:** serviço `self-service` (`_2_application`) chamando `GET /api/me`; o
  `hydrate` (session) passa a buscar o `self` e guardar o nome no `auth-store`. O
  avatar da sidebar passa a exibir a inicial do nome de exibição (`name ?? username`)
  no lugar do "C" fixo. Tooltip "Perfil" mantido.

## Acceptance criteria

- [ ] `User` possui `name` anulável e `Preferences` tipado (`theme` anulável,
      `language`/`locale` default `pt-BR`, `sidebarCollapsed` default `false`).
- [ ] Registro legado sem `name`/`preferences` carrega sem erro com padrões sensatos;
      seeder admin aplica os padrões.
- [ ] `UserUseCase.getMe` retorna `Result<_, DomainError>` na camada
      `security/_1_application`.
- [ ] Contexto de segurança expõe fachada (`implements Facade`) com `getMe`, wired em
      `SecurityModule`; ArchUnit verde (resource acessa contexto só via fachada).
- [ ] `GET /api/me` autenticado → `200` com `{ id, username, name, preferences{...} }`.
- [ ] `GET /api/me` sem token → `401`.
- [ ] Avatar da sidebar exibe a inicial do nome de exibição (ou do `username` na
      ausência de nome); tooltip "Perfil" preservado.
- [ ] Teste de unidade `UserUseCaseTest.getMe` sobre registro legado → `name` nulo +
      preferências padrão (prior art: `AccountUseCaseTest` + `InMemoryRepositories`).
- [ ] Teste de integração HTTP do recurso `self`: `GET /api/me` `200` e sem token
      `401` (prior art: `AccountResourceTest` sobre `BaseHttpTest`).

## Blocked by

None - can start immediately.
