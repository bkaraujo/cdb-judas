/* feature/import-rules.api.js — contrato público da fatia import-rules (equivalente ao FNNNApi
 * do backend). Único arquivo que outra fatia pode referenciar; delega para os arquivos internos
 * de feature/import-rules.js. Consumidores: transactions (form manual), import-statement
 * (preview de extrato/fatura) — casam a descrição digitada/importada contra as regras cacheadas. */
(function () {
  window.ImportRulesApi = {
    match:      function (description, rules) { return window.Domain.ImportRuleMatcher.match(description, rules); },
    listCached: function () { return window.App.ImportRuleService.listCached(); },
    // Única gravação autorizada em window.CBD.importRules fora da própria fatia — quick-create
    // no cabeçalho do preview de import-statement precisa ficar elegível na hora, sem esperar SSE.
    quickCreate: function (rule) {
      window.CBD = window.CBD || {};
      window.CBD.importRules = (window.CBD.importRules || []).concat([rule]);
    },
  };
})();
