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
    balance:      window.Infra.BalanceRepository.create(http),
    budget:       window.Infra.BudgetRepository.create(http),
    closing:      window.Infra.ClosingRepository.create(http),
    dashboard:    window.Infra.DashboardRepository.create(http),
    self:         window.Infra.SelfRepository.create(http),
  };

  const registryBootstrap = window.Infra.RegistryBootstrap.create({ repos: repos });
  const sse = window.Infra.SSEClient.create({
    baseUrl: baseUrl,
    authStore: window.Infra.AuthStore,
    bus: window.App.EventBus,
  });

  window.App.CacheStore.init({ bus: window.App.EventBus });
  window.App.SystemService.init({ api: http });
  window.App.SelfService.init({ repo: repos.self });
  // Write-through: preference changes push to the server via the self service (debounced).
  window.App.PreferencesService.init({
    storage: window.Infra.Storage,
    sync: function (patch) { return window.App.SelfService.updatePreferences(patch); },
  });

  window.App.AccountService.init     ({ repo: repos.accounts,     cache: window.App.CacheStore });
  window.App.CategoryService.init    ({ repo: repos.categories,   cache: window.App.CacheStore });
  window.App.TagService.init         ({ repo: repos.tags,         cache: window.App.CacheStore });
  window.App.CostCenterService.init  ({ repo: repos.costCenters,  cache: window.App.CacheStore });
  window.App.TransactionService.init ({ repo: repos.transactions });
  window.App.PayableService.init     ({ repo: repos.transactions });
  window.App.BalanceService.init     ({ repo: repos.balance });
  window.App.StatementService.init   ({ txRepo: repos.transactions, balance: window.App.BalanceService, cache: window.App.CacheStore });
  window.App.BudgetService.init      ({ repo: repos.budget });
  window.App.CreditCardService.init  ({ txRepo: repos.transactions, cache: window.App.CacheStore });
  window.App.ClosingService.init     ({ repo: repos.closing });
  window.App.DashboardService.init   ({ repo: repos.dashboard, txRepo: repos.transactions });
  const loginUrl = baseUrl.replace(/\/api\/?$/, '') + '/login';
  function loginFn(username, password) {
    return fetch(loginUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-request-id': window.Infra.HttpClient.reqId() },
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

  // Reads the self resource (/api/me) and stores the display name so the avatar/greeting
  // can show the user's identity. Tolerant: a failed /me must not block cadastro hydration.
  function hydrateSelf() {
    return window.App.SelfService.getMe()
      .then(function (me) {
        if (me) {
          window.Infra.AuthStore.setName(me.name || '');
          if (me.username) window.Infra.AuthStore.setUser(me.username);
          if (me.preferences) {
            // Reconcile: server wins (server theme null → client value is taught back).
            window.App.PreferencesService.applyServer(me.preferences);
            if (window.Theme && window.Theme.restore) window.Theme.restore();
            if (window.Sidebar && window.Sidebar.refreshState) window.Sidebar.refreshState();
          }
        }
        return me;
      })
      .catch(function () { return null; });
  }

  function hydrate() {
    return Promise.all([registryBootstrap.hydrate(), hydrateSelf()]).then(function () { return true; });
  }

  window.App.SessionService.init({ authStore: window.Infra.AuthStore, loginFn: loginFn, hydrate: hydrate, sse: sse });

  http.setUnauthorizedHandler(function () { window.App.SessionService.handleUnauthorized(); });

  // Exposed for shell/bootstrap to read after wiring.
  window.App._wiring = { http: http, repos: repos, sse: sse };
})();
