/* _2_application/budget-service.js — Budget use cases. */
(function () {
  let repo = null;

  function init(deps) { repo = deps.repo; return { ready: true }; }

  function loadPeriod(period) {
    return repo.list(period.month, period.year);
  }

  function save(id, data) { return repo.update(id, data); }
  function create(data)   { return repo.create(data); }
  function remove(id)     { return repo.remove(id); }

  function summary(items) {
    return {
      total:        (items || []).length,
      overspending: window.Domain.Budget.overspendCount(items),
    };
  }

  window.App = window.App || {};
  window.App.BudgetService = {
    init: init,
    loadPeriod: loadPeriod,
    save: save,
    create: create,
    remove: remove,
    summary: summary,
  };
})();
