/* _1_domain/credit-card.js — credit card rules (uses Account + Period). Pure. */
(function () {
  const DEFAULT_CLOSING_DAY = 1;
  const DEFAULT_DUE_DAY     = 10;

  function pad2(n) { return n < 10 ? '0' + n : '' + n; }

  /* Closing/due day are configured once per account (MON_ACCOUNT_LIMIT) and
     shared by every card on it — so these read from the account, not the card. */
  function closingDay(account) {
    const d = account && +account.closingDay;
    return d > 0 ? d : DEFAULT_CLOSING_DAY;
  }

  function dueDay(account) {
    const d = account && +account.dueDay;
    return d > 0 ? d : DEFAULT_DUE_DAY;
  }

  /* Janela de compras da fatura que VENCE em `period` — delega o ciclo a Domain.Invoice
     (contrato em docs/backend/invoice-cycle.md). Não é o mês-calendário: com closingDay=20 e
     dueDay=5, a fatura que vence em 05/04 cobre 21/02–20/03. Conta sem vencimento no período
     devolve uma janela vazia (from > to), o que zera os totais abaixo. */
  function invoicePeriod(account, period) {
    const dues = window.Domain.Invoice.dueDatesIn(account, period);
    if (!dues.length) return { from: '9999-12-31', to: '0000-01-01' };
    return window.Domain.Invoice.cycleFor(account, dues[0]);
  }

  function usagePct(used, limit) {
    const u = Math.abs(+used || 0);
    const l = +limit || 0;
    if (l <= 0) return 0;
    return Math.min(100, Math.max(0, (u / l) * 100));
  }

  function availableCredit(limit, used) {
    return Math.max(0, (+limit || 0) - Math.abs(+used || 0));
  }

  /* Bar color tokens by usage percent. Mirrors STYLE.md §11. */
  function barColorByUsage(pct) {
    if (pct >= 80) return 'expense';
    if (pct >= 60) return 'warning';
    return 'accent';
  }

  /* Total invoice = sum of |amount| over EXPENSE transactions inside the invoice cycle
     posted against this card (matched by `tx.cardId`, not the account). O ciclo é da conta,
     por isso ela entra na assinatura. */
  function invoiceTotal(transactions, cardId, account, period) {
    const b = invoicePeriod(account, period);
    const cid = String(cardId);
    return (transactions || []).reduce(function (acc, t) {
      if (String(t.cardId) !== cid) return acc;
      const dStr = String(t.date || '').slice(0, 10);
      if (dStr < b.from || dStr > b.to) return acc;
      const isExpense = String(t.type || '').toUpperCase() === 'EXPENSE' || (+t.amount || 0) < 0;
      if (!isExpense) return acc;
      return acc + Math.abs(+t.amount || 0);
    }, 0);
  }

  /* Total invoice for every card on the account combined — used for the shared
     usage bar (account.creditLimit is one limit for all of the account's cards). */
  function accountInvoiceTotal(transactions, account, period) {
    const b = invoicePeriod(account, period);
    const aid = String(account && account.id);
    return (transactions || []).reduce(function (acc, t) {
      if (String(t.accountId) !== aid || t.cardId == null) return acc;
      const dStr = String(t.date || '').slice(0, 10);
      if (dStr < b.from || dStr > b.to) return acc;
      const isExpense = String(t.type || '').toUpperCase() === 'EXPENSE' || (+t.amount || 0) < 0;
      if (!isExpense) return acc;
      return acc + Math.abs(+t.amount || 0);
    }, 0);
  }

  window.Domain = window.Domain || {};
  window.Domain.CreditCard = {
    DEFAULT_CLOSING_DAY: DEFAULT_CLOSING_DAY,
    DEFAULT_DUE_DAY:     DEFAULT_DUE_DAY,
    closingDay:          closingDay,
    dueDay:              dueDay,
    invoicePeriod:       invoicePeriod,
    usagePct:            usagePct,
    availableCredit:     availableCredit,
    barColorByUsage:     barColorByUsage,
    invoiceTotal:        invoiceTotal,
    accountInvoiceTotal: accountInvoiceTotal,
  };
})();
