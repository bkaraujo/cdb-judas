# Issue 06 — Extratos: detalhe por conta + resumo por conta

## Parent

PRD: `prd-reestruturacao-urls-multiusuario.md`

## What to build

Servir extratos sob o namespace de Contas em duas formas: o extrato detalhado de uma conta no mês (com saldo corrente linha a linha) e um novo resumo do mês por conta (saldo inicial, saldo final, total de entradas e saídas). O período vai no caminho no formato `yyyyMM`.

## Acceptance criteria

- [ ] `GET /api/{uuid}/accounts/{accId}/statements/{yyyyMM}` retorna o extrato detalhado com saldo corrente; filtro opcional por status
- [ ] `GET /api/{uuid}/accounts/statements/{yyyyMM}` retorna o resumo do mês por conta (id da conta, nome, saldo inicial, saldo final, total de entradas, total de saídas)
- [ ] Rota antiga `/api/statement` deixa de existir; repositório e página de extrato no front-end usam ambos os novos endpoints (panorama → detalhe)
- [ ] Testes: cálculo do resumo (saldo inicial/final e totais; mês vazio com inicial == final; presença de transferências; filtro de status); recurso de extrato em ambas as formas

## Blocked by

- Blocked by `issue-02-namespace-guarda-contas.md`
