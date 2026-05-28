# Issue 11 — Virada completa e endurecimento (cutover hardening)

## Parent

PRD: `prd-reestruturacao-urls-multiusuario.md`

## What to build

Consolidar a virada: tornar o prefixo de usuário o padrão no cliente (removendo o andaime de opt-in por chamada da transição), garantir que nenhuma rota antiga de dados sobreviva, atualizar a documentação da API e varrer os testes para o novo esquema.

## Acceptance criteria

- [ ] O `http-client` usa o prefixo `/{uuid}` por padrão; o andaime transitório de opt-in por chamada é removido; apenas chamadas globais (centro de custo) ficam explicitamente sem prefixo (o stream SSE usa o prefixo de usuário)
- [ ] Caminho de orçamento no front-end atualizado para `/api/{uuid}/budget` (sem back-end por ora)
- [ ] A documentação OpenAPI reflete o novo esquema de rotas
- [ ] Todas as classes `*ResourceTest` migradas; nenhuma rota de dados antiga `/api/*` remanescente
- [ ] Smoke completo: login → todas as telas carregam e operam sob o novo esquema

## Blocked by

- Blocked by `issue-02-namespace-guarda-contas.md`
- Blocked by `issue-03-cartoes-em-contas.md`
- Blocked by `issue-04-transacoes-namespace-topologia.md`
- Blocked by `issue-05-payables-filtro-transacoes.md`
- Blocked by `issue-06-extratos-resumo-detalhe.md`
- Blocked by `issue-07-fechamento-em-contas.md`
- Blocked by `issue-08-categorias-tags-namespace.md`
- Blocked by `issue-09-dashboard-namespace.md`
- Blocked by `issue-10-centro-custo-fixo.md`
