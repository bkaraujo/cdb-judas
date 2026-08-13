# CDB Finance — Guia Central

Gestor de finanças pessoais. Backend **Java 25 + Quarkus** (JVM mode); frontend **SPA Vanilla JS/CSS + jQuery 4** (servido pelo próprio backend). Arquitetura híbrida: **Vertical Slice** nas features de entrega HTTP (`br.cdb.feature.fNNN`) sobre **Hexagonal** nos contextos de negócio.

> **A documentação completa deste projeto mora na WikiJS** (skill `wikijs`), não mais em `docs/` ou nos `CLAUDE.md` de cada módulo — esses foram removidos/reduzidos em 2026-08-12. Este arquivo é só um apontador mínimo para orientar sessões futuras do Claude Code; para qualquer detalhe de arquitetura, padrão de código, contrato de API ou regra de negócio, **consulte a wiki**.

## Onde encontrar o quê

| Assunto | Página na wiki |
|---|---|
| Arquitetura backend (VSA+Hexagonal, módulos Maven, fatias `fNNN` f000–f999, Result, Lombok, Null-Safety, JDBC/H2, qualidade & build, testes, diagramas de pacote/classe/atividade, schema do banco) | `secular/profissao/bkraujo/judas/backend` |
| Arquitetura frontend (padrões 001–009, fatias físicas `web/feature/*`, contratos de API request/response JSON, seleção de categoria/tag, testes QUnit, diagramas) | `secular/profissao/bkraujo/judas/frontend` |
| Regras de negócio (funcionalidades, contas, cartões, ciclo de fatura, transações, categorização, importação de extrato/fatura, fechamento de período) | `secular/profissao/bkraujo/judas/regras-negocio` |
| Índice/overview do projeto | `secular/profissao/bkraujo/judas` |

Use a skill `wikijs` para ler/atualizar essas páginas (GraphQL API ou UI em `http://localhost:3000`). **Ao editar código, atualize a página correspondente na wiki** — não recrie `docs/` local.

## O que ainda vive no repositório (não migrado, uso operacional)

- **Inventário preciso de endpoints por fatia**: `br-application/src/main/java/br/cdb/feature/package-info.java` — fica ao lado do código de propósito, sempre o mais atual.
- **`README.md`** — visão geral do projeto e como executar (Maven/Docker).
- **`.claude/plan.md` / `.claude/refactor.md` / `.claude/frontend-refactor.md`** — histórico de migração, notas pessoais de sessão; gitignored, só na máquina local, não fazem parte da documentação do projeto.

## Esqueleto rápido (para não precisar abrir a wiki toda vez)

Módulos Maven: `br-parent` · `br-commons` · `br-context-people` + `br-context-monetary` (dissolvidos na fase 2 da migração — código vive hoje dentro das fatias `fNNN`) · `br-application` (`feature`/`core`/`infra`). `web/` não é módulo Maven, é copiado para `META-INF/resources` pelo pom de `br-application`.

Fatias existentes: `f000`–`f007`, `f009`, `f010`, `f999`. Cross-slice sempre via evento (`br.commons.MessageBus`), `f000.InternalApi`, ou adapter em `f999._2_infrastructure.adapter` — nunca import direto de fatia irmã. Detalhes completos: página **backend** acima.
