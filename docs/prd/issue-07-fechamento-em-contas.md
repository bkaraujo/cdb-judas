# Issue 07 — Fechamento sob o namespace de Contas

## Parent

PRD: `prd-reestruturacao-urls-multiusuario.md`

## What to build

Mover o recurso de fechamento (período de consolidação) para sob o namespace de Contas do usuário.

## Acceptance criteria

- [ ] `GET`/`POST`/`DELETE /api/{uuid}/accounts/closing` consultam, definem e limpam o período de fechamento
- [ ] Rota antiga `/api/operations/closing` deixa de existir; repositório de fechamento no front-end usa o novo caminho
- [ ] Fechamento funciona ponta a ponta pela UI no novo caminho
- [ ] Testes: recurso de fechamento no novo caminho

## Blocked by

- Blocked by `issue-02-namespace-guarda-contas.md`
