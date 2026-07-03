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

  function invoiceFor(cardId, period) {
    const b = window.Domain.Period.bounds(period);
    const params = 'from=' + b.from + '&to=' + b.to;
    return txRepo.list(params).then(function (txs) {
      const arr = Array.isArray(txs) ? txs : [];
      return {
        transactions: arr,
        total: window.Domain.CreditCard.invoiceTotal(arr, cardId, period),
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
