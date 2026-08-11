/* _2_application/credit-card-service.js — Credit card use cases. */
(function () {
  let txRepo = null;
  let cache = null;

  function init(deps) {
    txRepo = deps.txRepo;
    cache  = deps.cache;
    return { ready: true };
  }

  /* Every card across every account, flattened, each carrying its owning account
     (for color/name/limit) — used by views that group cards by account client-side. */
  function listFromCache() {
    const out = [];
    cache.accounts().forEach(function (a) {
      (a.cards || []).forEach(function (c) {
        out.push(Object.assign({}, c, { account: a }));
      });
    });
    return out;
  }

  /* Accounts that have at least one card — used by views that render one row/bar
     per account rather than per card (e.g. the dashboard panel). */
  function accountsWithCards() {
    return cache.accounts().filter(window.Domain.Account.hasCards);
  }

  /* Fatura que VENCE em `period`: as compras do ciclo (que começa no mês anterior — ver
     Domain.Invoice) lançadas contra este cartão. Devolve também o vencimento e o ciclo, que a
     tela de Extrato do Cartão usa no cabeçalho. Período sem vencimento → fatura vazia. */
  function invoiceFor(card, account, period) {
    const dues = window.Domain.Invoice.dueDatesIn(account, period);
    if (!dues.length) {
      return Promise.resolve({ dueDate: null, cycle: null, transactions: [], total: 0 });
    }
    const due = dues[0];
    const cycle = window.Domain.Invoice.cycleFor(account, due);
    return txRepo.listByAccount(account.id, 'dateFrom=' + cycle.from + '&dateTo=' + cycle.to)
      .then(function (txs) {
        const items = (Array.isArray(txs) ? txs : []).filter(function (t) {
          return String(t.cardId) === String(card.id);
        });
        return {
          dueDate: due,
          cycle: cycle,
          transactions: items,
          total: window.Domain.CreditCard.invoiceTotal(items, card.id, account, period),
        };
      });
  }

  window.App = window.App || {};
  window.App.CreditCardService = {
    init: init,
    listFromCache: listFromCache,
    accountsWithCards: accountsWithCards,
    invoiceFor: invoiceFor,
  };
})();
