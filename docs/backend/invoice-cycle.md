# Ciclo de fatura de cartão

Regra **duplicada de propósito** nas duas stacks — não há endpoint de fatura, e as duas precisam
dela para o mesmo mês bater dos dois lados:

| Stack | Arquivo |
|---|---|
| Backend | `br-application/src/main/java/br/cdb/feature/f002/_0_domain/model/InvoiceCycle.java` |
| Frontend | `web/js/_1_domain/invoice.js` (`window.Domain.Invoice`) |

Este documento é o contrato. **Mudou aqui, muda nos dois.**

## O ciclo é da conta, não do cartão

`closingDay`/`dueDay` são colunas de `F002_ACCOUNT` (`NUM_CLOSING_DAY`/`NUM_DUE_DAY`), compartilhadas
por todos os cartões da conta (`F003_CARD.COD_ACCOUNT`). Não configurados → **`closingDay = 1`,
`dueDay = 10`**.

## Definição

Com `close(m)` = dia `closingDay` do mês `m`, clampado ao tamanho do mês (`min(closingDay, len(m))`):

1. **Fechamento** — a compra `d` pertence ao ciclo `m` tal que `close(m-1) < d <= close(m)`.
   Intervalo meio-aberto: comprar **no** dia do fechamento ainda entra na fatura que fecha naquele dia.
2. **Vencimento** — `dueDate(m)` é a primeira data com dia `dueDay` (também clampado)
   **estritamente depois** de `close(m)`. Isso resolve sozinho o caso `dueDay <= closingDay`, em que
   a fatura vence no mês seguinte ao do fechamento.
3. **Inverso** — `cycleFor(dueDate)` devolve `{ from: close(m-1) + 1 dia, to: close(m) }`, onde
   `close(m)` é o último fechamento estritamente anterior a `dueDate`.

## Exemplos

`closingDay = 20`, `dueDay = 5`:

| Compra | Ciclo | Vencimento |
|---|---|---|
| 18/03/2026 | 21/02 – 20/03 | 05/04/2026 |
| 20/03/2026 | 21/02 – 20/03 | 05/04/2026 |
| 21/03/2026 | 21/03 – 20/04 | 05/05/2026 |

`closingDay = 1`, `dueDay = 10` (defaults):

| Compra | Ciclo | Vencimento |
|---|---|---|
| 01/03/2026 | 02/02 – 01/03 | 10/03/2026 |
| 15/03/2026 | 02/03 – 01/04 | 10/04/2026 |

`closingDay = 31`, fevereiro: `close(2026-02) = 28/02` (clamp).

## Onde a regra é aplicada

**Backend** — `f002.BalanceService.recalculate` re-data toda transação com `cardId` para o
vencimento antes de somar os snapshots mensais de `F002_BALANCE`. Compra de cartão só sai da conta
quando a fatura vence. `AccountResponse.currentBalance` **não** é afetado: soma tudo sem corte de
data, então re-datar não muda o total.

Mudança de regra não exige migração de schema: `FeatureBootstrap` chama
`BalanceService.recomputeAll()` no startup (marca todo snapshot sujo e recomputa), o que alcança
dados já gravados.

**Frontend** — `Domain.Invoice.collapse` substitui as transações de cartão por uma linha sintética
por `(cardId, vencimento)` nas telas de Lançamentos e Extrato de Contas, e
`Domain.CreditCard.invoicePeriod` usa o mesmo ciclo nos totais da tela de Cartões, do painel do
dashboard e do Extrato do Cartão (`#/card-statement/{cardId}`).

## Teste

Pela borda, como manda `docs/backend/testing.md` (sem teste de unidade de classe interna):
`F002AccountResourceTest` cria conta com `closingDay=20`/`dueDay=5` + cartão + compra em 25/03 e
verifica que `GET /accounts/{id}/balance?period=202603` não a contém e `?period=202605` contém.
