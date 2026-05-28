# Issue 08 — Categorias e Tags sob o namespace do usuário

## Parent

PRD: `prd-reestruturacao-urls-multiusuario.md`

## What to build

Mover os recursos de Categorias e Tags para o namespace do usuário, preservando o CRUD e a imutabilidade das categorias de sistema.

## Acceptance criteria

- [ ] Categorias em `/api/{uuid}/categories` (criar, listar, editar, excluir); categorias de sistema permanecem imutáveis (mutação/exclusão rejeitadas)
- [ ] Tags em `/api/{uuid}/tags` (criar, listar, editar, excluir)
- [ ] Rotas antigas deixam de existir; repositórios de categoria e tag no front-end atualizados
- [ ] Categorias e tags funcionam ponta a ponta pela UI nos novos caminhos
- [ ] Testes: recursos de categoria e de tag nos novos caminhos

## Blocked by

- Blocked by `issue-02-namespace-guarda-contas.md`
