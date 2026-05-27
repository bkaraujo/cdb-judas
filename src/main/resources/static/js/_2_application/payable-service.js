/* _2_application/payable-service.js — Payable / Receivable use cases. */
(function () {
  let repo = null;

  function init(deps) { repo = deps.repo; return { ready: true }; }

  function listPayable()        { return repo.listPayable(); }
  function listReceivable()     { return repo.listReceivable(); }
  function confirm(id, date)    { return repo.confirm(id, date); }
  function cancel(id)           { return repo.cancel(id); }

  function periodTotals(items, period) {
    return window.Domain.Payable.periodTotals(items, period);
  }

  function inPeriod(items, period) {
    return window.Domain.Payable.inPeriod(items, period);
  }

  window.App = window.App || {};
  window.App.PayableService = {
    init: init,
    listPayable: listPayable,
    listReceivable: listReceivable,
    confirm: confirm,
    cancel: cancel,
    periodTotals: periodTotals,
    inPeriod: inPeriod,
  };
})();
