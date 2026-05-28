# Issue 03 — Cartões de crédito absorvidos por Contas

## Parent

PRD: `prd-reestruturacao-urls-multiusuario.md`

## What to build

Tratar cartão de crédito como o subtipo de conta que ele é: criar/listar/editar/excluir cartões através do recurso de Contas (`type=card`), removendo o recurso de topo separado de cartões e a fiação correspondente no front-end.

## Acceptance criteria

- [ ] Criar um cartão via Contas com `type=card`, carregando os campos específicos (últimos 4 dígitos, dia de fechamento, dia de vencimento, limite, conta vinculada)
- [ ] Listar apenas cartões via Contas filtrando pelo tipo cartão
- [ ] Editar e excluir um cartão pelos endpoints de item de conta
- [ ] Recurso de cartão de crédito (back-end) e repositório de cartão (front-end) removidos
- [ ] Página de cartões funciona ponta a ponta através de Contas `type=card`
- [ ] Testes: contas cobrem criar/listar/editar/excluir cartão; teste do recurso de cartão removido

## Blocked by

- Blocked by `issue-02-namespace-guarda-contas.md`
