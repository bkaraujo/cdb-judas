/* feature/budget.api.js — contrato público da fatia budget (equivalente ao FNNNApi do backend).
 * Único arquivo que outra fatia pode referenciar. Consumidor: dashboard (painel de metas de
 * despesa — achado ao migrar dashboard, não estava no roadmap original). */
(function () {
  window.BudgetApi = {
    loadPeriod:     function (period) { return window.App.BudgetService.loadPeriod(period); },
    consumptionPct: function (spent, budgeted) { return window.Domain.Budget.consumptionPct(spent, budgeted); },
    isOverBudget:   function (spent, budgeted) { return window.Domain.Budget.isOverBudget(spent, budgeted); },
  };
})();
