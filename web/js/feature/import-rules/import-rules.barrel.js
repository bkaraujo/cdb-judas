/* feature/import-rules/import-rules.barrel.js — sequencia domain → application → secondary →
 * primary da própria fatia. Não é chamado por ninguém de fora.
 *
 * Sem *.api.js nesta rodada: os consumidores cross-slice de hoje (create-edit.js e
 * import-statement.js, ambos ainda legados em web/pages/transactions/) chamam
 * window.Domain.ImportRuleMatcher.match(...) e window.App.ImportRuleService.listCached()
 * diretamente — nomes preservados pela migração, então continuam funcionando sem alteração.
 * Ver V10/V11 em .claude/frontend-refactor.md para o retrofit previsto quando essas fatias
 * migrarem. */
(function () {
  const base = 'js/feature/import-rules/';
  const files = [
    '_0_domain/import-rule-matcher.js',
    '_1_application/import-rule-service.js',
    '_2_infrastructure/secondary/import-rule-repository.js',
    '_2_infrastructure/primary/page.js'
  ];
  files.forEach(function (path) {
    const s = document.createElement('script');
    s.src = base + path;
    s.async = false;
    document.head.appendChild(s);
  });
})();
