/* _2_application/credit-card-service.js — Credit card use cases. */
(function () {
  let repo = null;
  let txRepo = null;
  let cache = null;

  function init(deps) {
    repo   = deps.repo;
    txRepo = deps.txRepo;
    cache  = deps.cache;
    return { ready: true };
  }

  function listFromCache() {
    return cache.accounts().filter(window.Domain.Account.isCreditCard);
  }

  function listRemote() { return repo.list(); }

  function invoiceFor(cardId, period) {
    const b = window.Domain.Period.bounds(period);
    const params = 'from=' + b.from + '&to=' + b.to + '&accountId=' + encodeURIComponent(cardId);
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
    listRemote: listRemote,
    invoiceFor: invoiceFor,
  };
})();
