/* core/boot.js — único entrypoint do frontend, único <script> referenciado por index.html.
 * Ordem: kernel (core/kernel) → fatias (web/feature/<slice>.js, um arquivo por fatia, sem
 * barrel — cada bloco interno já é um IIFE independente) → composition-root (roda por último,
 * faz o wiring de DI). Espelha FeatureBootstrap.onStart() do backend: initialize(f000); fatias
 * em ordem; initialize(f999). Migração fatia-por-fatia concluída — sem bloco legado. */
(function () {
  function inject(src) {
    const s = document.createElement('script');
    s.src = src;
    s.async = false;
    document.head.appendChild(s);
  }

  inject('core/kernel/kernel.barrel.js');

  [
    'feature/reports.js',
    'feature/cost-centers.js',
    'feature/tags.js',
    'feature/categories.js',
    'feature/categories.api.js',
    'feature/settings.js',
    'feature/import-rules.js',
    'feature/import-rules.api.js',
    'feature/accounts.js',
    'feature/accounts.api.js',
    'feature/transactions.js',
    'feature/transactions.api.js',
    'feature/statement.js',
    'feature/credit-cards.js',
    'feature/credit-cards.api.js',
    'feature/accounts-payable.js',
    'feature/accounts-payable.api.js',
    'feature/import-statement.js',
    'feature/import-statement.api.js',
    'feature/budget.js',
    'feature/budget.api.js',
    'feature/dashboard.js'
  ].forEach(inject);

  inject('core/composition-root/composition-root.barrel.js');
})();
