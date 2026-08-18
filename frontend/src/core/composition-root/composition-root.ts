/** Wiring explícito Infra → Application → Fatias. Equivalente a `web/core/composition-root/
 * composition-root.js`, mas sem `window.*`: cada fatia só é alcançável aqui, via as factories
 * `createXService`/`createXRepository`/`createXApi` publicadas pelo `index.ts` dela — nunca por
 * nome global. `page-registry.ts` monta o `PageRegistry` a partir do objeto retornado aqui.
 *
 * `POST /login` fica fora de `/api` (fora do `HttpClient`) — mesma decisão do original
 * (`composition-root.js:72`).
 */
import {
  createCostCenterRepository,
  createCostCenterService,
} from '../../feature/cost-centers/index.ts';
import { createTagRepository, createTagService } from '../../feature/tags/index.ts';
import {
  createCategoriesApi,
  createCategoryRepository,
  createCategoryService,
} from '../../feature/categories/index.ts';
import {
  createImportRuleRepository,
  createImportRuleService,
  createImportRulesApi,
} from '../../feature/import-rules/index.ts';
import {
  createAccountRepository,
  createAccountService,
  createAccountsApi,
  createBalanceRepository,
  createBalanceService,
  createClosingDialog,
  createClosingRepository,
  createClosingService,
} from '../../feature/accounts/index.ts';
import { createCreditCardService, createCreditCardsApi } from '../../feature/credit-cards/index.ts';
import type { CreditCardsTxRepoPort } from '../../feature/credit-cards/index.ts';
import {
  createTransactionRepository,
  createTransactionService,
  createTransactionFormModal,
  createTransactionActions,
  createTransactionsApi,
} from '../../feature/transactions/index.ts';
import { createStatementService } from '../../feature/statement/index.ts';
import { createPayableService, createAccountsPayableApi } from '../../feature/accounts-payable/index.ts';
import { createImportStatementApi } from '../../feature/import-statement/index.ts';
import { createBudgetRepository, createBudgetService, createBudgetApi } from '../../feature/budget/index.ts';

import { buildPageRegistry } from './page-registry.ts';
import type { InvoiceTx } from '../kernel/_0_domain/invoice.ts';
import { createCacheStore } from '../kernel/_1_application/cache-store.ts';
import type { CacheStore } from '../kernel/_1_application/cache-store.ts';
import { createEventBus } from '../kernel/_1_application/event-bus.ts';
import { createPeriodService } from '../kernel/_1_application/period-service.ts';
import { createPreferencesService } from '../kernel/_1_application/preferences-service.ts';
import { createSelfService } from '../kernel/_1_application/self-service.ts';
import { createSessionService } from '../kernel/_1_application/session-service.ts';
import type { LoginResult, SessionService } from '../kernel/_1_application/session-service.ts';
import { createSystemService } from '../kernel/_1_application/system-service.ts';
import { createAuthStore } from '../kernel/_2_infrastructure/secondary/auth-store.ts';
import { createHttpClient } from '../kernel/_2_infrastructure/secondary/http-client.ts';
import { createRegistryBootstrap } from '../kernel/_2_infrastructure/secondary/registry-bootstrap.ts';
import { createSelfRepository } from '../kernel/_2_infrastructure/secondary/self-repository.ts';
import { createSseClient } from '../kernel/_2_infrastructure/secondary/sse-client.ts';
import { createStorage } from '../kernel/_2_infrastructure/secondary/storage.ts';
import { initUi } from '../kernel/_2_infrastructure/primary/ui/index.ts';
import { configureQuickCreate } from '../kernel/_2_infrastructure/primary/pickers.ts';
import { createSidebar } from '../kernel/_2_infrastructure/primary/sidebar.ts';
import type { Sidebar } from '../kernel/_2_infrastructure/primary/sidebar.ts';
import { createRouter } from '../kernel/_2_infrastructure/primary/router.ts';
import type { Router } from '../kernel/_2_infrastructure/primary/router.ts';
import { createTheme } from '../kernel/_2_infrastructure/primary/theme.ts';
import type { Theme } from '../kernel/_2_infrastructure/primary/theme.ts';

export interface App {
  cache: CacheStore;
  authStore: ReturnType<typeof createAuthStore>;
  preferences: ReturnType<typeof createPreferencesService>;
  theme: Theme;
  self: ReturnType<typeof createSelfService>;
  session: SessionService;
  sidebar: Sidebar;
  router: Router;
}

const API_BASE_URL = '/api';

export function createApp(): App {
  // ── Infra base ──────────────────────────────────────────────
  const storage = createStorage();
  const authStore = createAuthStore(storage);
  const http = createHttpClient({ authStore, baseUrl: API_BASE_URL });
  const bus = createEventBus();
  const cache = createCacheStore(bus);

  const selfRepo = createSelfRepository(http);
  const self = createSelfService(selfRepo);
  const preferences = createPreferencesService({
    storage,
    authStore,
    sync: (patch) => self.updatePreferences({ ...patch, theme: patch.theme ?? undefined }),
  });
  const system = createSystemService(http);
  const periodService = createPeriodService(storage);
  const theme = createTheme({ getTheme: () => preferences.getTheme(), setTheme: (t) => preferences.setTheme(t) });

  // ── Repositórios (um por fatia com secondary própria) ──────
  const accountRepo = createAccountRepository(http);
  const balanceRepo = createBalanceRepository(http);
  const closingRepo = createClosingRepository(http);
  const categoryRepo = createCategoryRepository(http);
  const tagRepo = createTagRepository(http);
  const costCenterRepo = createCostCenterRepository(http);
  const importRuleRepo = createImportRuleRepository(http);
  const transactionRepo = createTransactionRepository(http);
  const budgetRepo = createBudgetRepository(http);
  // dashboard não tem secondary própria consumida pela página (DashboardService/DashboardRepository
  // existem só pela fidelidade ao original — nenhum consumidor real; ver dashboard/page.ts).

  // ── Serviços (application) ─────────────────────────────────
  const accountService = createAccountService({ repo: accountRepo, cache });
  const balanceService = createBalanceService({ repo: balanceRepo });
  const closingService = createClosingService({ repo: closingRepo });
  const categoryService = createCategoryService({ repo: categoryRepo, cache });
  const tagService = createTagService({ repo: tagRepo, cache });
  const costCenterService = createCostCenterService({ repo: costCenterRepo, cache });
  const importRuleService = createImportRuleService({ repo: importRuleRepo, cache });
  const transactionService = createTransactionService({ repo: transactionRepo, cache });
  const budgetService = createBudgetService({ repo: budgetRepo });

  // Quick-create do kernel (pickers.ts) não pode referenciar CategoryService/TagService por
  // nome (fatia irmã) — composition-root é quem conhece os dois lados.
  configureQuickCreate('category', { create: (payload) => categoryService.create({ ...payload, parentId: payload.parentId ?? undefined }) });
  configureQuickCreate('tag', { create: (payload) => tagService.create(payload) });

  const importRulesApi = createImportRulesApi(importRuleService, cache);

  const transactionFormModal = createTransactionFormModal({ transactionService, cache, importRules: importRulesApi });
  const transactionActions = createTransactionActions({ transactionService, formModal: transactionFormModal });
  const transactionsApi = createTransactionsApi(transactionService, transactionActions);

  // Payable/Statement/CreditCard/Dashboard leem transações mas não são donas da fatia — recebem
  // TransactionsApi (contrato público), nunca o repositório cru de outra fatia. `TransactionResponse`
  // tem campos opcionais que os `InvoiceTx` locais de cada fatia (statement/credit-cards, dois
  // tipos distintos apesar do mesmo nome) exigem obrigatórios — na prática sempre presentes (o
  // backend nunca omite), daí o cast.
  const payableService = createPayableService({ repo: { list: (query) => transactionsApi.list(query) } });
  const statementService = createStatementService({
    txRepo: { listByAccount: (accountId, query) => transactionsApi.listByAccount(accountId, query) as unknown as Promise<InvoiceTx[] | null> },
    balance: { allAccounts: (period) => balanceService.allAccounts(period) },
    cache,
  });
  const creditCardService = createCreditCardService({
    txRepo: { listByAccount: (accountId, query) => transactionsApi.listByAccount(accountId, query) as unknown as ReturnType<CreditCardsTxRepoPort['listByAccount']> },
    cache,
  });

  const accountsApi = createAccountsApi(accountService, balanceService);
  const categoriesApi = createCategoriesApi(categoryService);
  const creditCardsApi = createCreditCardsApi(creditCardService);
  const accountsPayableApi = createAccountsPayableApi(payableService);
  const budgetApi = createBudgetApi(budgetService);
  const importStatementApi = createImportStatementApi(transactionsApi, importRulesApi, cache);

  const closingDialogFactory = createClosingDialog(closingService);

  // ── Sidebar / Router (ciclo real: sidebar precisa de router.go pro avatar/settings, router
  // precisa de sidebar.setCurrent pro item ativo — injetados um no outro via referência tardia,
  // mesmo padrão documentado em sidebar.ts/router.ts). ──
  let routerRef: Router | null = null;
  const sidebar = createSidebar({
    preferences: {
      getSidebarCollapsed: () => preferences.getSidebarCollapsed(),
      setSidebarCollapsed: (b) => preferences.setSidebarCollapsed(b),
      getSidebarGroups: () => preferences.getSidebarGroups() as Record<string, boolean>,
      setSidebarGroups: (obj) => preferences.setSidebarGroups(obj),
    },
    authStore,
    theme: { get: () => theme.get(), toggle: () => theme.toggle() },
    systemService: system,
    router: { go: (route) => routerRef?.go(route) },
  });
  // Fechamento contábil é da fatia accounts — kernel (sidebar.ts) não pode referenciar
  // ClosingService/closingDialog por nome.
  sidebar.configureClosing({
    get: () => closingService.get().then((r) => (r ? { period: r.period ?? null } : null)),
    openDialog: (opts) => {
      closingDialogFactory(opts);
    },
  });

  const registry = buildPageRegistry({
    cache,
    periodService,
    preferences,
    sidebar,
    authStore,
    self,
    theme: { get: () => theme.get(), set: (t) => theme.set(t) },
    accountService,
    payableService,
    budgetService,
    creditCardService,
    categoryService,
    costCenterService,
    importRuleService,
    statementService,
    tagService,
    transactionService,
    transactionActions,
    transactionsApi,
    accountsApi,
    accountsPayableApi,
    budgetApi,
    categoriesApi,
    creditCardsApi,
    importStatementApi,
  });

  const router = createRouter(registry, { sidebar: { setCurrent: (id) => sidebar.setCurrent(id) } });
  routerRef = router;

  // ── Sessão / bootstrap ──────────────────────────────────────
  const registryBootstrap = createRegistryBootstrap({
    categories: categoryRepo,
    accounts: accountRepo,
    tags: tagRepo,
    costCenters: costCenterRepo,
    importRules: importRuleRepo,
  });

  const sse = createSseClient({
    baseUrl: API_BASE_URL,
    authStore,
    bus,
    cache,
    enqueue: (fn) => http.enqueue(fn), // handshake do stream na mesma fila do token rotativo
  });

  const loginUrl = API_BASE_URL.replace(/\/api\/?$/, '') + '/login';
  function loginFn(username: string, password: string): Promise<LoginResult> {
    return fetch(loginUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-request-id': crypto.randomUUID() },
      body: JSON.stringify({ username, password }),
    }).then((res) => {
      if (!res.ok) {
        const err = new Error('HTTP ' + res.status) as Error & { status?: number };
        err.status = res.status;
        throw err;
      }
      return {
        token: res.headers.get('X-Access-Token') || '',
        userId: res.headers.get('X-User-Id') || '',
      };
    });
  }

  // Lê o recurso self (/api/me) e guarda o nome de exibição pro avatar/saudação. Tolerante: um
  // /me com falha não pode travar a hidratação do cadastro.
  function hydrateSelf() {
    return self
      .getMe()
      .then((me) => {
        if (me) {
          authStore.setName(me.name || '');
          if (me.username) authStore.setUser(me.username);
          sidebar.refreshClosing();
          if (me.preferences) {
            // Reconcilia: servidor vence (tema nulo no servidor → valor do cliente é ensinado de volta).
            preferences.applyServer(me.preferences);
            theme.restore();
            sidebar.refreshState();
          }
        }
        return me;
      })
      .catch(() => null);
  }

  function hydrate(): Promise<boolean> {
    return Promise.all([registryBootstrap.hydrate().then((snapshot) => cache.hydrate(snapshot)), hydrateSelf()]).then(() => true);
  }

  const session = createSessionService({ authStore, loginFn, hydrate, sse });
  http.setUnauthorizedHandler(() => session.handleUnauthorized());

  initUi(() => cache.tags());

  return { cache, authStore, preferences, theme, self, session, sidebar, router };
}
