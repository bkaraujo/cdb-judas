# CDB Finance — Guia Central

Gestor de finanças pessoais. Backend **Java 25 + Quarkus** (JVM mode); frontend **SPA TypeScript + Vite** em `frontend/` (jQuery 4 via npm, servido pelo próprio backend). Arquitetura híbrida: **Vertical Slice** nas features de entrega HTTP (`br.cdb.feature.fNNN`) sobre **Hexagonal** nos contextos de negócio.

> **A documentação completa deste projeto mora na WikiJS** (skill `wikijs`), não mais em `docs/` ou nos `CLAUDE.md` de cada módulo — esses foram removidos/reduzidos em 2026-08-12. Este arquivo é só um apontador mínimo para orientar sessões futuras do Claude Code; para qualquer detalhe de arquitetura, padrão de código, contrato de API ou regra de negócio, **consulte a wiki**.

## Onde encontrar o quê

> **Reorganizada em 2026-09-01**: as páginas monolíticas `backend`/`frontend` viraram **hubs** (só conteúdo transversal); endpoints/modelo/eventos/regras/telas de cada fatia migraram para uma **página por feature** em `secular/profissao/bkraujo/judas/feature/*`, que traz backend e frontend da mesma feature juntos. `br.cdb.core`/`br-commons` migraram para a página `core`.

| Assunto | Página na wiki |
|---|---|
| Hub backend (VSA+Hexagonal, módulos Maven, Result, Lombok, Null-Safety, JDBC/H2, qualidade & build, testes, cache off-heap por sessão, schema do banco, diagramas gerais) | `secular/profissao/bkraujo/judas/backend` |
| Hub frontend (padrões 001–010, contratos de API genéricos, seleção de categoria/tag, testes Vitest + gates `typecheck`/`check:arch`, diagramas gerais) | `secular/profissao/bkraujo/judas/frontend` |
| `br.cdb.core` (`event`/`persistence`/`security`/`web`) e `br-commons` | `secular/profissao/bkraujo/judas/core` |
| Feature Configurações — `f001` + `frontend/src/feature/settings` | `secular/profissao/bkraujo/judas/feature/settings` |
| Feature Contas & Cartões de Crédito — `f002`+`f003` + `accounts`+`credit-cards` | `secular/profissao/bkraujo/judas/feature/accounts-credit-cards` |
| Feature Tags — `f004` + `tags` | `secular/profissao/bkraujo/judas/feature/tags` |
| Feature Categorias — `f005` + `categories` | `secular/profissao/bkraujo/judas/feature/categories` |
| Feature Lançamentos & Extrato — `f006` + `transactions`+`statement`+`accounts-payable` | `secular/profissao/bkraujo/judas/feature/transactions` |
| Feature Importação de Extrato/Fatura — `f007` + `import-statement` | `secular/profissao/bkraujo/judas/feature/import-statement` |
| Feature Regras de Importação — `f010` + `import-rules` | `secular/profissao/bkraujo/judas/feature/import-rules` |
| Feature Dashboard — `f009` + `dashboard`+`budget` (assimetria: `budget` sem endpoint no backend) | `secular/profissao/bkraujo/judas/feature/dashboard` |
| Feature Relatórios — sem fatia backend própria; `reports`+`report-category-evolution` | `secular/profissao/bkraujo/judas/feature/reports` |
| Regras de negócio (funcionalidades, contas, cartões, ciclo de fatura, transações, categorização, importação de extrato/fatura, fechamento de período) | `secular/profissao/bkraujo/judas/regras-negocio` |
| Casos de uso — **página única**: tabela tela × endpoint (UI-001…UI-095 com diagrama de atividade cada; UC-001…UC-090 com objetivo, exceções, regras, endpoint/classe) | `secular/profissao/bkraujo/judas/casos-de-uso` |
| Índice/overview do projeto | `secular/profissao/bkraujo/judas` |

Use a skill `wikijs` para ler/atualizar essas páginas (GraphQL API ou UI em `http://localhost:3000`). **Ao editar código, atualize a página correspondente na wiki** — não recrie `docs/` local.

## O que ainda vive no repositório (não migrado, uso operacional)

- **Inventário preciso de endpoints por fatia**: `br-application/src/main/java/br/cdb/feature/package-info.java` — fica ao lado do código de propósito, sempre o mais atual.
- **`README.md`** — visão geral do projeto e como executar (Maven/Docker).
- **`.claude/plan.md` / `.claude/refactor.md` / `.claude/frontend-refactor.md`** — histórico de migração, notas pessoais de sessão; gitignored, só na máquina local, não fazem parte da documentação do projeto.

## Esqueleto rápido (para não precisar abrir a wiki toda vez)

Módulos Maven (três, é o que o `<modules>` da raiz lista): `br-parent` · `br-commons` · `br-application` (`feature`/`core` — **não há `br.cdb.infra`**). `br-context-people`/`br-context-monetary` foram dissolvidos na fase 2 e não existem mais nem como diretório. `frontend/` não é módulo Maven: o pom de `br-application` roda `npm ci`/`npm run build` no `generate-resources` e copia `frontend/dist` para `META-INF/resources` (`-Dexec.skip=true` pula o bundle). O SPA vanilla em `web/` foi removido em 2026-08-18.

Config de runtime mora em **`application.yaml` na raiz** (árvore `cdb.*`, lida por `CoreModule` via system property `cdb.application.yaml`); `application.properties` só cuida de `quarkus.*` — as chaves `datasource.jdbc.*`/`DATASOURCE_JDBC_URL` foram removidas de lá (a interface `DataSourceProperties` sobrou no código, sem consumidor). DDL é por fatia (`FNNNModule.model()` + `Database.initialize`), sem migrações one-shot: mudou schema, apague `database.mv.db`.

Fatias existentes: `f000`–`f007`, `f009`, `f010`, `f999`. Cross-slice sempre via evento (`br.commons.MessageBus`), `f000.InternalApi`, ou adapter em `f999._2_infrastructure.adapter` — nunca import direto de fatia irmã. Detalhes completos: página **backend** acima.
