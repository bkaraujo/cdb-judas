# Issue 05 — A Pagar/Receber como filtro de transações (eliminar Payables)

## Parent

PRD: `prd-reestruturacao-urls-multiusuario.md`

## What to build

Eliminar o recurso de Contas a Pagar/Receber e passar a servir "A Pagar" e "A Receber" como um filtro sobre transações pendentes. Baixar uma obrigação passa a ser a alteração de status da transação.

## Acceptance criteria

- [ ] "A Pagar" exibe transações de despesa pendentes; "A Receber" exibe transações de receita pendentes — via filtro da lista de transações (`status=pending` + `type=expense|income`)
- [ ] Confirmar um item usa a alteração de status da transação (com data de pagamento)
- [ ] Recurso/serviço/DTOs de payables e o repositório de payable (front-end) removidos; testes de payables removidos
- [ ] A página "A Pagar e Receber" funciona ponta a ponta sobre o filtro de transações

## Blocked by

- Blocked by `issue-04-transacoes-namespace-topologia.md`
