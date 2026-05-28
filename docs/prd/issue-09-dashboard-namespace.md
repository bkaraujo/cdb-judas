# Issue 09 — Dashboard sob o namespace do usuário

## Parent

PRD: `prd-reestruturacao-urls-multiusuario.md`

## What to build

Mover o recurso de Dashboard para o namespace do usuário.

## Acceptance criteria

- [ ] Dashboard em `/api/{uuid}/dashboard`
- [ ] Rota antiga deixa de existir; repositório de dashboard no front-end atualizado
- [ ] Dashboard carrega ponta a ponta pela UI no novo caminho
- [ ] Testes: recurso de dashboard no novo caminho

## Blocked by

- Blocked by `issue-02-namespace-guarda-contas.md`
