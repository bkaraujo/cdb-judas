# Issue 04 — Transações: namespace + topologia espelhando extratos

## Parent

PRD: `prd-reestruturacao-urls-multiusuario.md`

## What to build

Mover transações para a topologia que espelha extratos, sob o namespace de Contas: uma coleção entre contas (para operações de visão geral, transferência e importação) e uma forma por conta (para operações escopadas a uma conta). A criação por conta toma o id da conta a partir do caminho.

## Acceptance criteria

- [ ] `GET /api/{uuid}/accounts/transactions` lista entre contas com filtros: data inicial, data final, limite, status e tipo
- [ ] `POST /api/{uuid}/accounts/transactions/transfer` cria uma transferência entre duas contas
- [ ] Importação `preview`/`confirm` sob `/api/{uuid}/accounts/transactions/import`
- [ ] `GET /api/{uuid}/accounts/{accId}/transactions` lista as transações de uma conta (mesmos filtros)
- [ ] `POST /api/{uuid}/accounts/{accId}/transactions` cria com o id da conta vindo do caminho
- [ ] `PATCH`/`DELETE /api/{uuid}/accounts/{accId}/transactions/{txId}`; a exclusão preserva o parâmetro de modo
- [ ] `PATCH .../{txId}/status` altera o status informando a data de pagamento
- [ ] Rotas antigas `/api/transactions` deixam de existir; repositório e páginas de transações no front-end usam os novos caminhos
- [ ] Testes: recursos de transação e de importação nos novos caminhos

## Blocked by

- Blocked by `issue-02-namespace-guarda-contas.md`
