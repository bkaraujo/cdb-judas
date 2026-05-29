/* app/composition-root.js — wires Infra → Application services.
 *
 * Pages must reach the backend through window.App.<Service> only.
 * window.CBD remains the in-memory cadastro cache (populated by
 * Infra.CadastroBootstrap + Infra.SSEClient, read via App.CacheStore).
 */
(function () {
  window.Pages = window.Pages || {};
  window.CBD = window.CBD || {};

  const baseUrl = window.API_BASE_URL || '/api';
  const http    = window.Infra.HttpClient.create({ baseUrl: baseUrl });

  const repos = {
    accounts:     window.Infra.AccountRepository.create(http),
    categories:   window.Infra.CategoryRepository.create(http),
    tags:         window.Infra.TagRepository.create(http),
    costCenters:  window.Infra.CostCenterRepository.create(http),
    transactions: window.Infra.TransactionRepository.create(http),
    statement:    window.Infra.StatementRepository.create(http),
    budget:       window.Infra.BudgetRepository.create(http),
    closing:      window.Infra.ClosingRepository.create(http),
    dashboard:    window.Infra.DashboardRepository.create(http),
  };

  const cadastroBootstrap = window.Infra.CadastroBootstrap.create({ repos: repos });
  const sse = window.Infra.SSEClient.create({
    baseUrl: baseUrl,
    authStore: window.Infra.AuthStore,
    bus: window.App.EventBus,
  });

  window.App.CacheStore.init({ bus: window.App.EventBus });
  window.App.PreferencesService.init({ storage: window.Infra.Storage });

  window.App.AccountService.init     ({ repo: repos.accounts,     cache: window.App.CacheStore });
  window.App.CategoryService.init    ({ repo: repos.categories,   cache: window.App.CacheStore });
  window.App.TagService.init         ({ repo: repos.tags,         cache: window.App.CacheStore });
  window.App.CostCenterService.init  ({ repo: repos.costCenters,  cache: window.App.CacheStore });
  window.App.TransactionService.init ({ repo: repos.transactions });
  window.App.PayableService.init     ({ repo: repos.transactions });
  window.App.StatementService.init   ({ repo: repos.statement });
  window.App.BudgetService.init      ({ repo: repos.budget });
  window.App.CreditCardService.init  ({ txRepo: repos.transactions, cache: window.App.CacheStore });
  window.App.ClosingService.init     ({ repo: repos.closing });
  window.App.DashboardService.init   ({ repo: repos.dashboard, txRepo: repos.transactions });
  const loginUrl = baseUrl.replace(/\/api\/?$/, '') + '/login';
  function loginFn(username, password) {
    return fetch(loginUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: username, password: password }),
    }).then(function (res) {
      if (!res.ok) {
        const err = new Error('HTTP ' + res.status);
        err.status = res.status;
        throw err;
      }
      return {
        token: res.headers.get('X-Access-Token'),
        userId: res.headers.get('X-User-Id'),
      };
    });
  }

  window.App.SessionService.init({ authStore: window.Infra.AuthStore, loginFn: loginFn, hydrate: cadastroBootstrap.hydrate, sse: sse });

  http.setUnauthorizedHandler(function () { window.App.SessionService.handleUnauthorized(); });

  // Exposed for shell/bootstrap to read after wiring.
  window.App._wiring = { http: http, repos: repos, sse: sse };
})();
