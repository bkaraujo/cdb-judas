# Issue 10 — Centro de Custo fixo, global e somente leitura

## Parent

PRD: `prd-reestruturacao-urls-multiusuario.md`

## What to build

Transformar Centro de Custo em dado fixo do sistema: rota global somente leitura (sem `uuid`), servida de uma fonte global única, sem qualquer mutação via API. Ajustar o front-end para somente leitura.

## Acceptance criteria

- [ ] `GET /api/cost-center` retorna a lista fixa do sistema ("Fixo", "Variável") com os identificadores já existentes, somente leitura, global (sem `uuid`)
- [ ] Criação, edição e exclusão de centros de custo não são mais possíveis via API
- [ ] A lista é servida a partir do arquivo global de centros de custo
- [ ] Repositório de centro de custo no front-end somente leitura; página de centros de custo sem UI de CRUD; canal de eventos (SSE) de centro de custo removido
- [ ] Testes: centro de custo somente leitura (mutações rejeitadas/removidas)

## Blocked by

- Blocked by `issue-01-fundacao-identidade-migracao.md`
