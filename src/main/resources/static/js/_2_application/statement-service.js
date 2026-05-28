/* _2_application/statement-service.js — Statement use cases (detail + month summary). */
(function () {
  let repo = null;

  function init(deps) { repo = deps.repo; return { ready: true }; }

  // Period {month, year} → "yyyyMM".
  function yyyymm(period) {
    return String(period.year) + (period.month < 10 ? '0' : '') + period.month;
  }

  function load(accountId, period, status) {
    return repo.detail(accountId, yyyymm(period), status);
  }

  function summary(period, status) {
    return repo.summary(yyyymm(period), status);
  }

  window.App = window.App || {};
  window.App.StatementService = {
    init: init,
    load: load,
    summary: summary,
  };
})();
