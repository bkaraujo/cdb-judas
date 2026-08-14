/* core/manifest.js — lista única de arquivos do frontend (kernel + fatias), fonte de verdade
 * para core/kernel/kernel.barrel.js, core/boot.js e test/index.html — os três só carregavam
 * cópias manuais desta lista, que já haviam divergido (test/index.html não injetava
 * feature/card-statement.js). Caminhos são relativos a web/; cada consumidor aplica seu próprio
 * prefixo de base (kernel.barrel.js prefixa por camada, test/index.html prefixa '../'). */
window.CDB_MANIFEST = {
  kernel: {
    domain: [
      'period.js',
      'money.js',
      'category.js',
      'tag.js',
      'account.js',
      'invoice.js',
      'transaction.js',
      'statement-item.js'
    ],
    application: [
      'event-bus.js',
      'cache-store.js',
      'period-service.js',
      'preferences-service.js',
      'system-service.js',
      'self-service.js',
      'session-service.js'
    ],
    secondary: [
      'storage.js',
      'auth-store.js',
      'http-client.js',
      'self-repository.js',
      'registry-bootstrap.js',
      'sse-client.js'
    ],
    primary: [
      'icons.js',
      'format.js',
      'ui.js',
      'helpers.js',
      'pickers.js',
      'delete-dialog.js',
      'theme.js',
      'sidebar.data.js',
      'sidebar.js',
      'router.js'
    ]
  },
  feature: [
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
    'feature/card-statement.js',
    'feature/accounts-payable.js',
    'feature/accounts-payable.api.js',
    'feature/import-statement.js',
    'feature/import-statement.api.js',
    'feature/budget.js',
    'feature/budget.api.js',
    'feature/dashboard.js'
  ]
};
